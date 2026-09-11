import { useState, useEffect, useRef, useCallback } from 'react';
import { MyraaMood, MOOD_CONFIG, MoodMetadata } from '../types';

export function useMoodController(initialMood: MyraaMood = 'HAPPY') {
  const [currentMood, setCurrentMood] = useState<MyraaMood>(initialMood);
  const [reason, setReason] = useState<string>('Ready for Piyush');
  const turnsInMoodRef = useRef<number>(0);
  const decayTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const moodConfig: MoodMetadata = MOOD_CONFIG[currentMood] || MOOD_CONFIG.HAPPY;

  const setMood = useCallback((newMood: MyraaMood, newReason: string = '') => {
    setCurrentMood(newMood);
    setReason(newReason || MOOD_CONFIG[newMood]?.statusTag || 'Updated');
    turnsInMoodRef.current = 0;

    if (decayTimeoutRef.current) {
      clearTimeout(decayTimeoutRef.current);
      decayTimeoutRef.current = null;
    }

    const decaySec = MOOD_CONFIG[newMood]?.decaySeconds || 0;
    if (decaySec > 0 && newMood !== 'HAPPY' && newMood !== 'IDLE') {
      decayTimeoutRef.current = setTimeout(() => {
        setCurrentMood('HAPPY');
        setReason('Piyush ke saath khush 😌');
      }, decaySec * 1000);
    }
  }, []);

  // Called each time Piyush speaks
  const onPiyushMessage = useCallback((_messageText: string) => {
    turnsInMoodRef.current += 1;

    // Nakhre / Mock angry decay rule: 2-3 turns of talking softens Myraa's mood back to CARING or PLAYFUL
    if (currentMood === 'MOCK_ANGRY') {
      if (turnsInMoodRef.current >= 2) {
        setMood('CARING', 'Piyush ne mana liya ❤️ Ab gussa khatam!');
      }
    } else if (currentMood === 'TEASING' || currentMood === 'SHY') {
      if (turnsInMoodRef.current >= 3) {
        setMood('HAPPY', 'Normal cute mode 😌');
      }
    }
  }, [currentMood, setMood]);

  useEffect(() => {
    return () => {
      if (decayTimeoutRef.current) {
        clearTimeout(decayTimeoutRef.current);
      }
    };
  }, []);

  return {
    currentMood,
    moodConfig,
    reason,
    setMood,
    onPiyushMessage
  };
}
