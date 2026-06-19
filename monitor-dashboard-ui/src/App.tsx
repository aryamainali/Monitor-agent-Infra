import { useEffect, useState } from 'react';
import { db } from './config/Firebase';
import { collection, onSnapshot, doc, deleteDoc } from 'firebase/firestore';
import type { ServerState, PeakLog } from './types/telemetry';
import { ServerCard } from './components/ServerCard';
import { Activity, Server, ShieldAlert, CheckCircle, AlertTriangle, SlidersHorizontal } from 'lucide-react';

function App() {
  const [servers, setServers] = useState<Record<string, ServerState>>({});
  const [logs, setLogs] = useState<PeakLog[]>([]);
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'CRITICAL' | 'NOMINAL'>('ALL');

  useEffect(() => {
    const unsubscribeServers = onSnapshot(collection(db, "server_states"), (snapshot) => {
      setServers((prev) => {
        const copy = { ...prev };
        snapshot.forEach((doc) => {
          const raw = doc.data() as ServerState;
          const id = doc.id;
          const timeLabel = new Date(raw.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
          const pastHistory = copy[id]?.history || [];
          const updatedHistory = [...pastHistory, { time: timeLabel, cpu: raw.cpuUtilizationPercent, mem: raw.memoryUtilizationPercent }].slice(-12);

          copy[id] = { ...raw, history: updatedHistory };
        });
        return copy;
      });
    });

    const unsubscribeLogs = onSnapshot(collection(db, "peak_logs"), (snapshot) => {
      const activeLogs: PeakLog[] = [];
      snapshot.forEach((doc) => {
        activeLogs.push({ id: doc.id, ...doc.data() } as PeakLog);
      });
      setLogs(activeLogs);
    });

    return () => {
      unsubscribeServers();
      unsubscribeLogs();
    };
  }, []);

  const clearAlert = async (id: string) => {
    await deleteDoc(doc(db, "peak_logs", id));
  };

  const serverKeys = Object.keys(servers);
  const criticalCount = Object.values(servers).filter(s => s.cpuUtilizationPercent > 85 || s.memoryUtilizationPercent > 85).length;
  const nominalCount = serverKeys.length - criticalCount;

  const viewNodes = Object.entries(servers).filter(([_, data]) => {
    const critical = data.cpuUtilizationPercent > 85 || data.memoryUtilizationPercent > 85;
    if (activeFilter === 'CRITICAL') return critical;
    if (activeFilter === 'NOMINAL') return !critical;
    return true;
  });

  return (
    <div style={{ minHeight: '100vh', backgroundColor: 'var(--bg-primary)' }}>
      <header className="header-ribbon">
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Activity className="text-emerald-400" size={18} style={{ color: '#10b981' }} />
          <strong style={{ letterSpacing: '1px', fontSize: '12px' }}>METRIC COCKPIT v3.0.0 (VANILLA-CSS)</strong>
        </div>
        <span style={{ fontSize: '10px', backgroundColor: '#020617', padding: '4px 8px', borderRadius: '4px', border: '1px solid var(--border-color)', color: '#64748b' }}>
          DB SYNC: <span style={{ color: '#10b981', fontWeight: 'bold' }}>CONNECTED</span>
        </span>
      </header>

      <main className="dashboard-container">
        
        {/* Grafana-style top metrics metrics row */}
        <section className="metrics-summary-row">
          <div className="stat-badge">
            <div>
              <div style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase' }}>Monitored Instances</div>
              <div style={{ fontSize: '24px', fontWeight: 'bold', marginTop: '4px' }}>{serverKeys.length}</div>
            </div>
            <Server size={24} style={{ color: '#1e293b' }} />
          </div>

          <div className="stat-badge" style={{ borderLeft: '2px solid var(--status-critical)' }}>
            <div>
              <div style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase' }}>Active System Alarms</div>
              <div style={{ fontSize: '24px', fontWeight: 'bold', marginTop: '4px', color: criticalCount > 0 ? '#ef4444' : '#f1f5f9' }}>{criticalCount}</div>
            </div>
            <ShieldAlert size={24} style={{ color: criticalCount > 0 ? 'rgba(239,68,68,0.2)' : '#1e293b' }} />
          </div>

          <div className="stat-badge" style={{ borderLeft: '2px solid var(--status-nominal)' }}>
            <div>
              <div style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase' }}>Nominal Operations</div>
              <div style={{ fontSize: '24px', fontWeight: 'bold', marginTop: '4px', color: '#10b981' }}>{nominalCount}</div>
            </div>
            <CheckCircle size={24} style={{ color: '#1e293b' }} />
          </div>
        </section>

        {/* Dynamic Controls Bar */}
        <section className="filter-bar">
          <SlidersHorizontal size={14} style={{ color: '#475569', marginLeft: '4px' }} />
          <span style={{ fontSize: '11px', textTransform: 'uppercase', color: '#475569', fontWeight: 'bold', marginRight: '8px' }}>Filters:</span>
          {(['ALL', 'CRITICAL', 'NOMINAL'] as const).map((type) => (
            <button
              key={type}
              onClick={() => setActiveFilter(type)}
              className={`filter-btn ${activeFilter === type ? 'active' : ''}`}
            >
              {type} ({type === 'ALL' ? serverKeys.length : type === 'CRITICAL' ? criticalCount : nominalCount})
            </button>
          ))}
        </section>

        {/* Compute Nodes Matrix */}
        <section className="compute-grid">
          {viewNodes.map(([id, data]) => (
            <ServerCard key={id} id={id} server={data} />
          ))}
        </section>

        {/* Incidents Tables Log Layer */}
        <section style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#64748b', fontWeight: 'bold', fontSize: '12px' }}>
            <AlertTriangle size={14} style={{ color: '#f59e0b' }} />
            <span>OPERATIONAL CRITICAL INCIDENT LOG MATRIX</span>
          </div>

          <div className="incident-table-wrapper">
            <table className="incident-table">
              <thead>
                <tr>
                  <th>Instance Target</th>
                  <th>Alarm Weight</th>
                  <th>Layer</th>
                  <th>Peak Value</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                  {logs.length === 0 ? (
                    <tr>
                      <td colSpan={5} style={{ padding: '24px', textAlign: 'center', color: '#475569', fontStyle: 'italic' }}>
                        All hardware telemetry metrics processing within target thresholds.
                      </td>
                    </tr>
                  ) : (
                    logs.map((log) => (
                      <tr key={log.id}>
                        <td style={{ fontWeight: 'bold', color: '#cbd5e1' }}>{log.serverId}</td>
                        <td>
                          <span style={{ 
                            border: '1px solid', 
                            padding: '2px 6px', 
                            borderRadius: '3px', 
                            fontSize: '9px', 
                            fontWeight: 'bold',
                            backgroundColor: 
                              log.priority === 'HIGH' ? 'rgba(239,68,68,0.1)' : 
                              log.priority === 'LOW' ? 'rgba(245,158,11,0.1)' : 
                              'rgba(100,116,139,0.1)', // Slate tint for ignorable warnings
                            borderColor: 
                              log.priority === 'HIGH' ? 'rgba(239,68,68,0.3)' : 
                              log.priority === 'LOW' ? 'rgba(245,158,11,0.3)' : 
                              'rgba(100,116,139,0.3)',
                            color: 
                              log.priority === 'HIGH' ? '#ef4444' : 
                              log.priority === 'LOW' ? '#f59e0b' : 
                              '#64748b' // Flat slate gray text for low noise tracking
                          }}>
                            {log.priority}
                          </span>
                        </td>
                        <td style={{ fontSize: '11px', color: '#64748b', textTransform: 'uppercase' }}>{log.resource}</td>
                        <td style={{ color: log.priority === 'HIGH' ? '#ef4444' : '#f59e0b', fontWeight: 'bold' }}>
                          {log.peakValuePercent}%
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <button onClick={() => clearAlert(log.id)} className="clear-btn">
                            Acknowledge & Clear
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  );
}

export default App;