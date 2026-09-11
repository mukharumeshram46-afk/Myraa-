import React, { useState } from 'react';
import { useMoodController } from './hooks/useMoodController';
import { MoodAuraOrb } from './components/MoodAuraOrb';
import { AvatarView } from './components/AvatarView';
import { AndroidToolsHUD } from './components/AndroidToolsHUD';
import { AudioDiagnostics } from './components/AudioDiagnostics';
import { MyraaMood, MOOD_CONFIG, DiagnosticsState } from './types';
import { Heart, Sparkles, Mic, MicOff, RefreshCw, Send } from 'lucide-react';

export const App: React.FC = () => {
  const { currentMood, moodConfig, reason, setMood, onPiyushMessage } = useMoodController('HAPPY');
  const [connectionState, setConnectionState] = useState<string>('CONNECTED');
  const [amplitude, setAmplitude] = useState<number>(0.2);
  const [inputText, setInputText] = useState<string>('');
  const [chatLog, setChatLog] = useState<Array<{ sender: string; text: string }>>([
    { sender: 'MYRAA', text: 'Hlo Piyush... kya kar rahe ho? 😌' }
  ]);

  const diagnostics: DiagnosticsState = {
    model: 'gemini-3.1-flash-live-preview',
    voice: 'Aoede (Feminine)',
    websocketStatus: 'CONNECTED',
    audioDuplex: 'Active (PCM16 / 24kHz)',
    activeToolsCount: 16,
    batteryHealth: '84% (Good)'
  };

  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim()) return;

    const userText = inputText.trim();
    setChatLog((prev) => [...prev, { sender: 'Piyush', text: userText }]);
    setInputText('');

    // Trigger mood decay / response in controller
    onPiyushMessage(userText);

    // Simulate responsive Myraa reply
    setTimeout(() => {
      let reply = "Haan Piyush, main hamesha aapke saath hoon ❤️";
      if (currentMood === 'MOCK_ANGRY') {
        reply = "Hmph... thik hai, maan gayi! Par agli baar time pe aana 😤❤️";
      } else if (currentMood === 'CUTE') {
        reply = "Aap kitne sweet ho Piyush... 🥺❤️";
      } else if (currentMood === 'TEASING') {
        reply = "Accha ji? Mujhe pata tha aap mere bina bore ho jaoge 😏";
      }
      setChatLog((prev) => [...prev, { sender: 'MYRAA', text: reply }]);
      setAmplitude(0.65);
      setTimeout(() => setAmplitude(0.15), 1500);
    }, 600);
  };

  const handleTriggerTool = (toolName: string) => {
    setChatLog((prev) => [
      ...prev,
      { sender: 'System', text: `Tool executed: ${toolName}` },
      { sender: 'MYRAA', text: `Done Piyush 😌 ${toolName} execute kar diya phone pe!` }
    ]);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-purple-500 selection:text-white">
      {/* Top Navbar */}
      <header className="border-b border-white/10 bg-black/40 backdrop-blur-md sticky top-0 z-50 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div
            className="w-10 h-10 rounded-full flex items-center justify-center text-xl shadow-lg transition-colors duration-500"
            style={{
              backgroundColor: `${moodConfig.auraColor}33`,
              border: `1px solid ${moodConfig.auraColor}`
            }}
          >
            {moodConfig.emoji}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-bold text-lg tracking-wide text-white">MYRAA</h1>
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 border border-purple-500/30">
                v2.0
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Piyush's Autonomous Voice Companion & Copilot
            </p>
          </div>
        </div>

        {/* Status Badge */}
        <div className="flex items-center gap-3">
          <div className="hidden sm:flex flex-col items-end">
            <span className="text-xs font-medium text-white/90">{reason}</span>
            <span className="text-[10px] text-slate-400">Aoede Voice • Gemini Live</span>
          </div>
          <button
            onClick={() => {
              setConnectionState((prev) => (prev === 'CONNECTED' ? 'DISCONNECTED' : 'CONNECTED'));
            }}
            className={`p-2.5 rounded-xl border transition-all ${
              connectionState === 'CONNECTED'
                ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-400'
                : 'bg-rose-500/20 border-rose-500/40 text-rose-400'
            }`}
          >
            {connectionState === 'CONNECTED' ? <Mic className="w-4 h-4" /> : <MicOff className="w-4 h-4" />}
          </button>
        </div>
      </header>

      {/* Main Content Dashboard */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 space-y-6">
        {/* Top Hero Section: Avatar & Mood Aura */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-center bg-white/5 border border-white/10 rounded-3xl p-6 backdrop-blur-2xl relative overflow-hidden">
          {/* Background Ambient Glow */}
          <div
            className="absolute -right-20 -top-20 w-96 h-96 rounded-full filter blur-3xl opacity-30 pointer-events-none transition-all duration-700"
            style={{ backgroundColor: moodConfig.auraColor }}
          />

          {/* Left: Avatar View */}
          <div className="flex flex-col items-center justify-center">
            <AvatarView moodConfig={moodConfig} connectionState={connectionState} />
            <div className="mt-3 text-center">
              <span className="text-sm font-semibold text-white/90">
                Current Mood: <span style={{ color: moodConfig.auraColor }}>{moodConfig.mood}</span>
              </span>
              <p className="text-xs text-slate-400">{moodConfig.statusTag}</p>
            </div>
          </div>

          {/* Right: Holographic Aura Orb & Live Mood State Machine */}
          <div className="flex flex-col items-center justify-center">
            <MoodAuraOrb
              moodConfig={moodConfig}
              amplitude={amplitude}
              connectionState={connectionState}
            />

            {/* Mood Switcher Pills */}
            <div className="w-full">
              <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2 text-center flex items-center justify-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-purple-400" />
                Mood Matrix (Dynamic Decay Active)
              </div>
              <div className="flex flex-wrap justify-center gap-1.5 max-w-md mx-auto">
                {(Object.keys(MOOD_CONFIG) as MyraaMood[]).map((moodKey) => (
                  <button
                    key={moodKey}
                    onClick={() => setMood(moodKey)}
                    className={`text-xs px-2.5 py-1 rounded-full border transition-all ${
                      currentMood === moodKey
                        ? 'border-white bg-white/20 text-white font-semibold shadow-md'
                        : 'border-white/10 bg-black/30 text-white/70 hover:border-white/30'
                    }`}
                  >
                    {MOOD_CONFIG[moodKey].emoji} {moodKey}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Native Android 16 Tools HUD */}
        <AndroidToolsHUD onTriggerTool={handleTriggerTool} />

        {/* Audio Diagnostics & Live Telemetry */}
        <AudioDiagnostics diagnostics={diagnostics} amplitude={amplitude} />

        {/* Live Conversation Stream with Piyush */}
        <div className="bg-white/5 border border-white/10 rounded-2xl p-4 backdrop-blur-xl">
          <h3 className="text-sm font-semibold text-white/90 mb-3 flex items-center gap-2">
            <Heart className="w-4 h-4 text-rose-400" />
            Live Full-Duplex Dialogue with Piyush
          </h3>

          <div className="space-y-2.5 max-h-48 overflow-y-auto pr-2 mb-4">
            {chatLog.map((chat, idx) => (
              <div
                key={idx}
                className={`flex gap-2.5 ${chat.sender === 'Piyush' ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`p-3 rounded-2xl max-w-lg text-xs leading-relaxed ${
                    chat.sender === 'Piyush'
                      ? 'bg-purple-600/40 border border-purple-500/30 text-purple-100 rounded-tr-none'
                      : chat.sender === 'System'
                      ? 'bg-white/5 border border-white/10 text-slate-400 font-mono text-[11px]'
                      : 'bg-white/10 border border-white/10 text-white rounded-tl-none shadow-md'
                  }`}
                >
                  <div className="text-[10px] font-semibold opacity-70 mb-0.5">
                    {chat.sender}
                  </div>
                  {chat.text}
                </div>
              </div>
            ))}
          </div>

          <form onSubmit={handleSendMessage} className="flex gap-2">
            <input
              type="text"
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              placeholder="Baat karo Myraa se (e.g. 'Arey suno na...', 'Open WhatsApp')..."
              className="flex-1 bg-black/40 border border-white/15 rounded-xl px-4 py-2.5 text-xs text-white placeholder-white/40 focus:outline-none focus:border-purple-500"
            />
            <button
              type="submit"
              className="px-4 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-semibold flex items-center gap-1.5 transition"
            >
              <Send className="w-3.5 h-3.5" />
              Bolo
            </button>
          </form>
        </div>
      </main>
    </div>
  );
};
export default App;
