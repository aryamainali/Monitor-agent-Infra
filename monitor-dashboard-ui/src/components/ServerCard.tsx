import React from 'react';
import type { ServerState } from '../types/telemetry';
import { AreaChart, Area, XAxis, YAxis, ResponsiveContainer } from 'recharts';
import { Cpu, HardDrive } from 'lucide-react';

interface ServerCardProps {
  id: string;
  server: ServerState;
}

export const ServerCard: React.FC<ServerCardProps> = ({ id, server }) => {
  const cpu = server.cpuUtilizationPercent || 0;
  const mem = server.memoryUtilizationPercent || 0;
  const isCritical = cpu > 85 || mem > 85;

  const getMetricColor = (val: number) => {
    if (val >= 85) return '#ef4444';
    if (val >= 70) return '#f59e0b';
    return '#10b981';
  };

  return (
    <div className={`panel-card ${isCritical ? 'critical-edge' : ''}`}>
      <div className="panel-header">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="pulse-indicator" style={{ backgroundColor: isCritical ? '#ef4444' : '#10b981' }}></span>
            <strong style={{ fontSize: '14px', color: '#e2e8f0' }}>{id}</strong>
          </div>
          <span style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase' }}>{server.osName}</span>
        </div>
        <div style={{ fontSize: '10px', color: '#64748b' }}>
          {new Date(server.timestamp).toLocaleTimeString()}
        </div>
      </div>

      <div className="gauge-row">
        <div className="gauge-box">
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '10px', color: '#64748b', marginBottom: '4px' }}>
            <Cpu size={12} /> <span>CPU LOAD</span>
          </div>
          <span style={{ fontSize: '20px', fontWeight: 'bold', color: getMetricColor(cpu) }}>
            {cpu.toFixed(1)}<span style={{ fontSize: '11px', color: '#475569' }}>%</span>
          </span>
          <div className="progress-track">
            <div className="progress-fill" style={{ width: `${cpu}%`, backgroundColor: getMetricColor(cpu) }}></div>
          </div>
        </div>

        <div className="gauge-box">
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '10px', color: '#64748b', marginBottom: '4px' }}>
            <HardDrive size={12} /> <span>RAM LOAD</span>
          </div>
          <span style={{ fontSize: '20px', fontWeight: 'bold', color: getMetricColor(mem) }}>
            {mem.toFixed(1)}<span style={{ fontSize: '11px', color: '#475569' }}>%</span>
          </span>
          <div className="progress-track">
            <div className="progress-fill" style={{ width: `${mem}%`, backgroundColor: getMetricColor(mem) }}></div>
          </div>
        </div>
      </div>

      {/* Embedded Sparkline History Timeline with explicitly configured gradients inside tags */}
      <div style={{ height: '80px', width: '100%', background: 'rgba(2,6,18,0.2)', border: '1px solid #0f172a', padding: '4px', borderRadius: '4px' }}>
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={server.history || []} margin={{ top: 5, right: 0, left: -20, bottom: 0 }}>
            <defs>
              <linearGradient id={`cpuG-${id}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#10b981" stopOpacity={0.2}/>
                <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
              </linearGradient>
              <linearGradient id={`memG-${id}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.2}/>
                <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
              </linearGradient>
            </defs>
            <XAxis dataKey="time" hide />
            <YAxis domain={[0, 100]} hide />
            <Area type="monotone" dataKey="cpu" stroke="#10b981" fillOpacity={1} fill={`url(#cpuG-${id})`} strokeWidth={1.5} />
            <Area type="monotone" dataKey="mem" stroke="#3b82f6" fillOpacity={1} fill={`url(#memG-${id})`} strokeWidth={1.5} />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};