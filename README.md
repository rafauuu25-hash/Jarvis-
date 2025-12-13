import React, { useState, useEffect, useRef } from 'react';
import { Mic, MicOff, Power, Activity, Cpu, Zap } from 'lucide-react';

export default function JarvisAssistant() {
  const [isActive, setIsActive] = useState(false);
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [response, setResponse] = useState('');
  const [loading, setLoading] = useState(false);
  const [waveform, setWaveform] = useState(Array(20).fill(0));
  const canvasRef = useRef(null);

  useEffect(() => {
    if (isActive) {
      const interval = setInterval(() => {
        setWaveform(prev => prev.map(() => Math.random() * 100));
      }, 100);
      return () => clearInterval(interval);
    }
  }, [isActive]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    
    ctx.clearRect(0, 0, width, height);
    
    // Círculos concêntricos animados
    const centerX = width / 2;
    const centerY = height / 2;
    const time = Date.now() / 1000;
    
    for (let i = 0; i < 5; i++) {
      const radius = 50 + i * 30 + Math.sin(time + i) * 10;
      const alpha = isActive ? 0.3 - i * 0.05 : 0.1 - i * 0.02;
      
      ctx.beginPath();
      ctx.arc(centerX, centerY, radius, 0, Math.PI * 2);
      ctx.strokeStyle = `rgba(100, 200, 255, ${alpha})`;
      ctx.lineWidth = 2;
      ctx.stroke();
    }
    
    // Núcleo central
    ctx.beginPath();
    ctx.arc(centerX, centerY, 20, 0, Math.PI * 2);
    ctx.fillStyle = isActive ? 'rgba(100, 200, 255, 0.8)' : 'rgba(100, 200, 255, 0.3)';
    ctx.fill();
    
    requestAnimationFrame(() => {});
  }, [waveform, isActive]);

  const handleVoiceInput = async () => {
    if (!isActive) return;
    
    setIsListening(true);
    setTranscript('');
    setResponse('');
    
    // Simula reconhecimento de voz
    setTimeout(() => {
      const commands = [
        "Qual é o status dos sistemas?",
        "Mostre-me o clima hoje",
        "Ative o protocolo de segurança",
        "Analise os dados do reator",
        "Prepare o traje Mark 50"
      ];
      const randomCommand = commands[Math.floor(Math.random() * commands.length)];
      setTranscript(randomCommand);
      setIsListening(false);
      processCommand(randomCommand);
    }, 2000);
  };

  const processCommand = async (command) => {
    setLoading(true);
    
    try {
      const response = await fetch("https://api.anthropic.com/v1/messages", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          model: "claude-sonnet-4-20250514",
          max_tokens: 1000,
          messages: [
            { 
              role: "user", 
              content: `Você é JARVIS, o assistente virtual do Tony Stark. Responda de forma concisa, técnica e elegante como JARVIS faria. Comando: ${command}`
            }
          ],
        })
      });

      const data = await response.json();
      const jarvisResponse = data.content[0].text;
      setResponse(jarvisResponse);
      
      // Simula voz
      if ('speechSynthesis' in window) {
        const utterance = new SpeechSynthesisUtterance(jarvisResponse);
        utterance.lang = 'pt-BR';
        utterance.rate = 0.9;
        utterance.pitch = 1;
        window.speechSynthesis.speak(utterance);
      }
    } catch (error) {
      setResponse("Desculpe senhor, houve um erro no processamento. Sistemas em recalibração.");
    }
    
    setLoading(false);
  };

  return (
    <div className="min-h-screen bg-black text-cyan-400 font-mono p-8 overflow-hidden relative">
      {/* Grid de fundo */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: 'linear-gradient(rgba(100, 200, 255, 0.3) 1px, transparent 1px), linear-gradient(90deg, rgba(100, 200, 255, 0.3) 1px, transparent 1px)',
          backgroundSize: '50px 50px'
        }}></div>
      </div>

      <div className="relative z-10 max-w-6xl mx-auto">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-6xl font-bold mb-2 tracking-wider" style={{
            textShadow: '0 0 20px rgba(100, 200, 255, 0.8)'
          }}>
            J.A.R.V.I.S
          </h1>
          <p className="text-sm text-cyan-300 tracking-widest">
            Just A Rather Very Intelligent System
          </p>
        </div>

        {/* Canvas de visualização */}
        <div className="flex justify-center mb-8">
          <canvas 
            ref={canvasRef} 
            width={400} 
            height={400}
            className="rounded-full"
            style={{
              background: 'radial-gradient(circle, rgba(0,20,40,0.8) 0%, rgba(0,0,0,0.9) 100%)'
            }}
          ></canvas>
        </div>

        {/* Controles */}
        <div className="flex justify-center gap-6 mb-8">
          <button
            onClick={() => setIsActive(!isActive)}
            className={`p-6 rounded-full transition-all duration-300 ${
              isActive 
                ? 'bg-cyan-500 bg-opacity-20 border-2 border-cyan-400' 
                : 'bg-gray-800 border-2 border-gray-600'
            }`}
            style={{
              boxShadow: isActive ? '0 0 30px rgba(100, 200, 255, 0.6)' : 'none'
            }}
          >
            <Power className={isActive ? 'text-cyan-400' : 'text-gray-500'} size={32} />
          </button>
          
          <button
            onClick={handleVoiceInput}
            disabled={!isActive || isListening || loading}
            className={`p-6 rounded-full transition-all duration-300 ${
              isActive && !isListening && !loading
                ? 'bg-cyan-500 bg-opacity-20 border-2 border-cyan-400 hover:bg-opacity-30' 
                : 'bg-gray-800 border-2 border-gray-600 opacity-50'
            }`}
            style={{
              boxShadow: isListening ? '0 0 30px rgba(100, 200, 255, 0.8)' : 'none'
            }}
          >
            {isListening ? <MicOff size={32} /> : <Mic size={32} />}
          </button>
        </div>

        {/* Status dos sistemas */}
        <div className="grid grid-cols-3 gap-4 mb-8">
          <div className="bg-cyan-950 bg-opacity-30 border border-cyan-700 p-4 rounded">
            <div className="flex items-center gap-2 mb-2">
              <Cpu size={20} />
              <span className="text-sm">CPU</span>
            </div>
            <div className="text-2xl font-bold">
              {isActive ? '87%' : '--'}
            </div>
          </div>
          
          <div className="bg-cyan-950 bg-opacity-30 border border-cyan-700 p-4 rounded">
            <div className="flex items-center gap-2 mb-2">
              <Activity size={20} />
              <span className="text-sm">STATUS</span>
            </div>
            <div className="text-2xl font-bold">
              {isActive ? 'ONLINE' : 'OFFLINE'}
            </div>
          </div>
          
          <div className="bg-cyan-950 bg-opacity-30 border border-cyan-700 p-4 rounded">
            <div className="flex items-center gap-2 mb-2">
              <Zap size={20} />
              <span className="text-sm">ENERGIA</span>
            </div>
            <div className="text-2xl font-bold">
              {isActive ? '100%' : '--'}
            </div>
          </div>
        </div>

        {/* Área de transcrição e resposta */}
        <div className="space-y-4">
          {transcript && (
            <div className="bg-blue-950 bg-opacity-30 border border-blue-700 p-4 rounded">
              <div className="text-xs text-blue-400 mb-2">COMANDO DETECTADO:</div>
              <div className="text-lg">{transcript}</div>
            </div>
          )}
          
          {loading && (
            <div className="bg-cyan-950 bg-opacity-30 border border-cyan-700 p-4 rounded">
              <div className="flex items-center gap-2">
                <div className="animate-pulse">⚡</div>
                <span>Processando requisição...</span>
              </div>
            </div>
          )}
          
          {response && !loading && (
            <div className="bg-cyan-950 bg-opacity-30 border border-cyan-700 p-4 rounded">
              <div className="text-xs text-cyan-400 mb-2">JARVIS:</div>
              <div className="text-lg leading-relaxed">{response}</div>
            </div>
          )}
        </div>

        {/* Wave form visual */}
        {isActive && (
          <div className="flex justify-center gap-1 mt-8">
            {waveform.map((height, i) => (
              <div
                key={i}
                className="w-2 bg-cyan-400 rounded-full transition-all duration-100"
                style={{
                  height: `${isListening ? height : 10}px`,
                  opacity: isListening ? 0.8 : 0.3,
                  boxShadow: isListening ? '0 0 10px rgba(100, 200, 255, 0.8)' : 'none'
                }}
              ></div>
            ))}
          </div>
        )}

        {!isActive && (
          <div className="text-center text-cyan-600 mt-8">
            Pressione o botão de energia para ativar JARVIS
          </div>
        )}
      </div>
    </div>
  );
}
