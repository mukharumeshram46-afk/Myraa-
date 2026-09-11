import React from 'react';
import {
  Smartphone,
  MousePointer,
  Type,
  MoveVertical,
  Sliders,
  Eye,
  MessageSquare,
  Workflow,
  Code2,
  Activity,
  MapPin,
  FileText,
  Camera,
  Moon,
  Sparkles,
  Search
} from 'lucide-react';

interface ToolItem {
  id: string;
  name: string;
  description: string;
  icon: React.ReactNode;
  category: 'System' | 'Vision' | 'Memory' | 'Companion';
}

const TOOLS_LIST: ToolItem[] = [
  { id: '1', name: 'android_openApp', description: 'Launch any installed app', icon: <Smartphone className="w-4 h-4 text-sky-400" />, category: 'System' },
  { id: '2', name: 'android_clickElement', description: 'Tap text, viewId, or coordinates', icon: <MousePointer className="w-4 h-4 text-emerald-400" />, category: 'System' },
  { id: '3', name: 'android_typeText', description: 'Input text & press enter', icon: <Type className="w-4 h-4 text-yellow-400" />, category: 'System' },
  { id: '4', name: 'android_scroll', description: 'Scroll UP/DOWN/LEFT/RIGHT', icon: <MoveVertical className="w-4 h-4 text-indigo-400" />, category: 'System' },
  { id: '5', name: 'android_systemAction', description: 'BACK, HOME, RECENTS, LOCK', icon: <Sliders className="w-4 h-4 text-pink-400" />, category: 'System' },
  { id: '6', name: 'android_readScreenNodes', description: 'Inspect live UI hierarchy', icon: <Eye className="w-4 h-4 text-cyan-400" />, category: 'Vision' },
  { id: '7', name: 'android_filterAndReplyNotification', description: 'Auto-reply to WhatsApp & Telegram', icon: <MessageSquare className="w-4 h-4 text-lime-400" />, category: 'System' },
  { id: '8', name: 'android_executeAppChain', description: 'Multi-step cross-app automation', icon: <Workflow className="w-4 h-4 text-violet-400" />, category: 'System' },
  { id: '9', name: 'vision_extractCodeAndErrors', description: 'OCR stack traces & compiler logs', icon: <Code2 className="w-4 h-4 text-teal-400" />, category: 'Vision' },
  { id: '10', name: 'system_monitorDeviceHealth', description: 'Battery, charging & CPU thermal', icon: <Activity className="w-4 h-4 text-rose-400" />, category: 'System' },
  { id: '11', name: 'location_checkContext', description: 'Geofence context (gym, office, home)', icon: <MapPin className="w-4 h-4 text-amber-400" />, category: 'System' },
  { id: '12', name: 'call_captureQuickNote', description: 'Structured notes & action items', icon: <FileText className="w-4 h-4 text-blue-400" />, category: 'Companion' },
  { id: '13', name: 'camera_takeQuickSnap', description: 'Headless FRONT/BACK camera snap', icon: <Camera className="w-4 h-4 text-fuchsia-400" />, category: 'Vision' },
  { id: '14', name: 'companion_eveningDebrief', description: 'Daily achievements & relaxation check', icon: <Moon className="w-4 h-4 text-purple-400" />, category: 'Companion' },
  { id: '15', name: 'memory_saveImportant', description: 'Store long-term facts for Piyush', icon: <Sparkles className="w-4 h-4 text-amber-300" />, category: 'Memory' },
  { id: '16', name: 'memory_query', description: 'Query saved memories & preferences', icon: <Search className="w-4 h-4 text-sky-300" />, category: 'Memory' }
];

interface AndroidToolsHUDProps {
  onTriggerTool?: (toolName: string) => void;
}

export const AndroidToolsHUD: React.FC<AndroidToolsHUDProps> = ({ onTriggerTool }) => {
  return (
    <div className="bg-white/5 border border-white/10 rounded-2xl p-4 backdrop-blur-xl">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-white/90 flex items-center gap-2">
          <Smartphone className="w-4 h-4 text-purple-400" />
          Native Android Tools Registry (16)
        </h3>
        <span className="text-[11px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 font-mono">
          All Services Active
        </span>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2.5 max-h-72 overflow-y-auto pr-1">
        {TOOLS_LIST.map((tool) => (
          <button
            key={tool.id}
            onClick={() => onTriggerTool?.(tool.name)}
            className="flex items-start gap-2.5 p-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/5 hover:border-purple-500/30 transition text-left group"
          >
            <div className="p-1.5 rounded-lg bg-black/40 border border-white/10 shrink-0 group-hover:scale-110 transition-transform">
              {tool.icon}
            </div>
            <div className="min-w-0">
              <div className="text-xs font-mono font-medium text-white/90 truncate group-hover:text-purple-300">
                {tool.name}
              </div>
              <div className="text-[10px] text-white/50 line-clamp-1">
                {tool.description}
              </div>
            </div>
          </button>
        ))}
      </div>
    </div>
  );
};
