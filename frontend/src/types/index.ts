export type MyraaMood =
  | 'IDLE'
  | 'HAPPY'
  | 'CUTE'
  | 'SHY'
  | 'PLAYFUL'
  | 'TEASING'
  | 'MOCK_ANGRY'
  | 'CARING'
  | 'PROUD'
  | 'EXCITED'
  | 'SLEEPY'
  | 'THINKING';

export interface MoodMetadata {
  mood: MyraaMood;
  emoji: string;
  auraColor: string;
  secondaryAura: string;
  statusTag: string;
  decaySeconds: number;
}

export const MOOD_CONFIG: Record<MyraaMood, MoodMetadata> = {
  IDLE: {
    mood: 'IDLE',
    emoji: '✨',
    auraColor: '#94A3B8',
    secondaryAura: '#64748B',
    statusTag: 'Online for Piyush',
    decaySeconds: 0
  },
  HAPPY: {
    mood: 'HAPPY',
    emoji: '🥰',
    auraColor: '#F59E0B',
    secondaryAura: '#FBBF24',
    statusTag: 'Khush & Cheerful',
    decaySeconds: 0
  },
  CUTE: {
    mood: 'CUTE',
    emoji: '🥺',
    auraColor: '#F472B6',
    secondaryAura: '#FB7185',
    statusTag: 'Aapki Myraa ❤️',
    decaySeconds: 15
  },
  SHY: {
    mood: 'SHY',
    emoji: '🙈',
    auraColor: '#FDBA74',
    secondaryAura: '#FB923C',
    statusTag: 'Sharma Gayi...',
    decaySeconds: 12
  },
  PLAYFUL: {
    mood: 'PLAYFUL',
    emoji: '😜',
    auraColor: '#A855F7',
    secondaryAura: '#C084FC',
    statusTag: 'Masti Mode',
    decaySeconds: 20
  },
  TEASING: {
    mood: 'TEASING',
    emoji: '😏',
    auraColor: '#D946EF',
    secondaryAura: '#E879F9',
    statusTag: 'Thode Nakhre 😏',
    decaySeconds: 18
  },
  MOCK_ANGRY: {
    mood: 'MOCK_ANGRY',
    emoji: '😤',
    auraColor: '#F43F5E',
    secondaryAura: '#E11D48',
    statusTag: 'Katti! Manau mujhe pehle 😤',
    decaySeconds: 25
  },
  CARING: {
    mood: 'CARING',
    emoji: '🩺',
    auraColor: '#10B981',
    secondaryAura: '#34D399',
    statusTag: 'Care & Comfort for Piyush',
    decaySeconds: 25
  },
  PROUD: {
    mood: 'PROUD',
    emoji: '😎',
    auraColor: '#6366F1',
    secondaryAura: '#818CF8',
    statusTag: 'Proud of Piyush!',
    decaySeconds: 15
  },
  EXCITED: {
    mood: 'EXCITED',
    emoji: '🤩',
    auraColor: '#FB923C',
    secondaryAura: '#F97316',
    statusTag: 'Super Excited!',
    decaySeconds: 15
  },
  SLEEPY: {
    mood: 'SLEEPY',
    emoji: '🥱',
    auraColor: '#818CF8',
    secondaryAura: '#A5B4FC',
    statusTag: 'Neend Aa Rahi Hai...',
    decaySeconds: 30
  },
  THINKING: {
    mood: 'THINKING',
    emoji: '🤔',
    auraColor: '#06B6D4',
    secondaryAura: '#22D3EE',
    statusTag: 'Deep In Thought...',
    decaySeconds: 10
  }
};

export type ConnectionState = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'LISTENING' | 'SPEAKING' | 'ERROR';

export interface DiagnosticsState {
  model: string;
  voice: string;
  websocketStatus: string;
  audioDuplex: string;
  activeToolsCount: number;
  batteryHealth: string;
}
