import React, { useState, useRef, useEffect } from 'react';
import { MessageCircle, X, Minimize2, Maximize2, Send, Search, Menu, Battery, Wifi, Signal, Loader2 } from 'lucide-react';
import { initializeApp } from 'firebase/app';
import { getAuth, signInWithCustomToken, signInAnonymously } from 'firebase/auth';
import { getFirestore, collection, query, onSnapshot, addDoc, serverTimestamp } from 'firebase/firestore';

const firebaseConfig = typeof __firebase_config !== 'undefined' ? JSON.parse(__firebase_config) : {};
const appId = typeof __app_id !== 'undefined' ? __app_id : 'default-app-id';
const initialAuthToken = typeof __initial_auth_token !== 'undefined' ? __initial_auth_token : null;

const MockBrowser = () => (
  <div className="w-full h-full bg-white text-gray-800 overflow-y-auto pt-8">
    <div className="bg-gray-100 p-2 border-b flex items-center gap-2 sticky top-0 z-10">
      <div className="bg-white rounded-full px-4 py-2 text-sm text-gray-500 flex-1 flex items-center gap-2 shadow-sm">
        <div className="w-4 h-4 bg-green-500 rounded-full flex items-center justify-center text-[10px] text-white">🔒</div>
        wikipedia.org/wiki/Multiverse
      </div>
      <Menu size={20} className="text-gray-600" />
    </div>
    
    <div className="p-4 space-y-4">
      <h1 className="text-2xl font-serif font-bold text-black border-b pb-2">Multiverse (Science)</h1>
      <div className="w-full h-48 bg-gray-200 rounded flex items-center justify-center text-gray-400">
        [Image Placeholder: Diagram of parallel universes]
      </div>
      <p className="font-serif leading-relaxed">
        The multiverse is a hypothetical group of multiple universes. Together, these universes comprise everything that exists: the entirety of space, time, matter, energy, information, and the physical laws and constants that describe them.
      </p>
      <p className="font-serif leading-relaxed">
        Different universes within the multiverse are called "parallel universes", "other universes", "alternate universes", or "many worlds".
      </p>
      <div className="p-4 bg-blue-50 border-l-4 border-blue-500 my-4">
        <h3 className="font-bold text-blue-800 mb-1">Quantum Mechanics</h3>
        <p className="text-sm">Many-worlds interpretation implies that all possible alternate histories and futures are real.</p>
      </div>
      <p className="font-serif leading-relaxed">
        The concept of multiple universes has been proposed in physics, astronomy, religion, philosophy, transpersonal psychology, and fiction.
      </p>
      {[...Array(10)].map((_, i) => (
        <p key={i} className="font-serif leading-relaxed text-gray-600">
          Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
        </p>
      ))}
    </div>
  </div>
);

const HomeScreen = ({ onLaunchOverlay }) => (
  <div className="w-full h-full bg-slate-900 text-white flex flex-col items-center justify-center p-6 relative overflow-hidden">
    {/* Decorative background circles */}
    <div className="absolute top-[-50px] left-[-50px] w-64 h-64 bg-blue-500 rounded-full blur-[100px] opacity-20"></div>
    <div className="absolute bottom-[-50px] right-[-50px] w-64 h-64 bg-purple-500 rounded-full blur-[100px] opacity-20"></div>

    <div className="z-10 text-center space-y-8">
      <div className="w-24 h-24 bg-gradient-to-tr from-cyan-400 to-blue-600 rounded-2xl flex items-center justify-center shadow-xl mx-auto mb-4 border border-white/20">
        <MessageCircle size={48} className="text-white" />
      </div>
      
      <div>
        <h1 className="text-3xl font-bold tracking-tight mb-2">Simple Rick AI</h1>
        <p className="text-slate-400">Always visible. Always ready.</p>
      </div>

      <button 
        onClick={onLaunchOverlay}
        className="bg-white text-slate-900 px-8 py-3 rounded-full font-bold shadow-lg hover:bg-cyan-50 transition-all active:scale-95 flex items-center gap-2 mx-auto"
      >
        Enable Overlay <Maximize2 size={18} />
      </button>

      <div className="text-xs text-slate-500 max-w-xs mx-auto mt-8">
        Simulates Android `SYSTEM_ALERT_WINDOW` permission behavior.
      </div>
    </div>
  </div>
);

// --- The Floating Overlay Component ---
const Overlay = ({ onClose, db, userId }) => {
  const [position, setPosition] = useState({ x: 20, y: 150 });
  const [isExpanded, setIsExpanded] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
  const [messages, setMessages] = useState([]);
  const [inputText, setInputText] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const overlayRef = useRef(null);
  const chatEndRef = useRef(null);

  // Constants for API and Firestore
  const API_KEY = ""; // Canvas provides key at runtime
  const API_URL = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-09-2025:generateContent?key=${API_KEY}`;
  // Private collection path: /artifacts/{appId}/users/{userId}/{collectionName}
  const CHAT_COLLECTION = `artifacts/${appId}/users/${userId}/rick_chat_history`;
  const MAX_RETRIES = 3;
  const SYSTEM_PROMPT = "You are Simple Rick, the kindest and most simple-minded Rick Sanchez in the multiverse. You remember previous interactions. Respond to all queries with cheerful, simplistic, and overly positive advice. Keep responses brief.";

  useEffect(() => {
    if (!db || !userId) {
        return;
    }

    const q = collection(db, CHAT_COLLECTION);

    const unsubscribe = onSnapshot(q, (snapshot) => {
      try {
        const fetchedMessages = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data(),
          timestamp: doc.data().timestamp?.toDate().getTime() || 0
        }));

        fetchedMessages.sort((a, b) => a.timestamp - b.timestamp);

        setMessages(fetchedMessages.map(msg => ({
          id: msg.id,
          text: msg.text,
          sender: msg.role === 'user' ? 'user' : 'bot'
        })));
      } catch (error) {
        console.error("Error processing Firestore snapshot:", error);
      }
    }, (error) => {
      console.error("Error listening to Firestore:", error);
    });

    return () => unsubscribe();
  }, [db, userId]);

  useEffect(() => {
    if (chatEndRef.current) {
      chatEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isExpanded]);


  const handlePointerDown = (e) => {
    const target = e.target;
    if (isExpanded && !target.closest('.drag-handle')) return;
    
    e.preventDefault(); 
    setIsDragging(true);
    
    const clientX = e.touches ? e.touches[0].clientX : e.clientX;
    const clientY = e.touches ? e.touches[0].clientY : e.clientY;
    
    const rect = overlayRef.current.getBoundingClientRect();
    setDragOffset({
      x: clientX - rect.left,
      y: clientY - rect.top
    });
  };

  useEffect(() => {
    const handlePointerMove = (e) => {
      if (!isDragging) return;
      
      const clientX = e.touches ? e.touches[0].clientX : e.clientX;
      const clientY = e.touches ? e.touches[0].clientY : e.clientY;

      let newX = clientX - dragOffset.x;
      let newY = clientY - dragOffset.y;

      const currentWidth = isExpanded ? 300 : 64;
      const currentHeight = isExpanded ? 400 : 64;
      
      const maxX = window.innerWidth - currentWidth;
      const maxY = window.innerHeight - currentHeight;
      
      newX = Math.max(0, Math.min(newX, maxX));
      newY = Math.max(0, Math.min(newY, maxY));

      setPosition({ x: newX, y: newY });
    };

    const handlePointerUp = () => {
      setIsDragging(false);
    };

    if (isDragging) {
      window.addEventListener('mousemove', handlePointerMove);
      window.addEventListener('mouseup', handlePointerUp);
      window.addEventListener('touchmove', handlePointerMove, { passive: false });
      window.addEventListener('touchend', handlePointerUp);
    }

    return () => {
      window.removeEventListener('mousemove', handlePointerMove);
      window.removeEventListener('mouseup', handlePointerUp);
      window.removeEventListener('touchmove', handlePointerMove);
      window.removeEventListener('touchend', handlePointerUp);
    };
  }, [isDragging, dragOffset, isExpanded]);

  const toggleExpand = () => {
    if (!isDragging) setIsExpanded(!isExpanded);
  };


  const prepareChatHistory = (currentQuery) => {
    const historyForApi = messages.map(msg => ({
      role: msg.sender === 'user' ? 'user' : 'model', 
      parts: [{ text: msg.text }]
    }));
    
    historyForApi.push({
      role: 'user',
      parts: [{ text: currentQuery }]
    });
    
    return historyForApi;
  };

  const callGeminiApi = async (queryText) => {
    setIsLoading(true);

    try {
      await addDoc(collection(db, CHAT_COLLECTION), {
        text: queryText,
        role: 'user',
        timestamp: serverTimestamp()
      });
    } catch (error) {
      console.error("Error saving user message to Firestore:", error);
      setIsLoading(false);
      return;
    }

    const history = prepareChatHistory(queryText);

    const payload = {
      contents: history,
      systemInstruction: { parts: [{ text: SYSTEM_PROMPT }] },
      tools: [{ "google_search": {} }], 
    };

    let responseText = "Aw geez, Morty! My brain is on the fritz!";
    let attempt = 0;

    while (attempt < MAX_RETRIES) {
      try {
        const response = await fetch(API_URL, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        const candidate = result.candidates?.[0];

        if (candidate && candidate.content?.parts?.[0]?.text) {
          responseText = candidate.content.parts[0].text;
          break; 
        } else {
          throw new Error("Gemini response structure invalid or empty.");
        }
      } catch (error) {
        console.error(`Attempt ${attempt + 1} failed:`, error.message);
        attempt++;
        if (attempt < MAX_RETRIES) {
          const delayTime = 2 ** attempt * 1000; 
          await new Promise(resolve => setTimeout(resolve, delayTime));
        } else {
          responseText = "Uh oh, Morty! The network got too complicated. I can't talk right now!";
        }
      }
    }

    try {
      await addDoc(collection(db, CHAT_COLLECTION), {
        text: responseText,
        role: 'model',
        timestamp: serverTimestamp()
      });
    } catch (error) {
      console.error("Error saving model response to Firestore:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const sendMessage = (e) => {
    e.preventDefault();
    const queryText = inputText.trim();
    if (!queryText || isLoading) return;

    setInputText('');
    callGeminiApi(queryText);
  };
  
  if (!isExpanded) {
    return (
      <div 
        ref={overlayRef}
        style={{ left: position.x, top: position.y }}
        onMouseDown={handlePointerDown}
        onTouchStart={handlePointerDown}
        onClick={toggleExpand}
        className="fixed z-50 cursor-pointer touch-none transition-transform active:scale-95 hover:scale-105"
      >
        <div className="w-16 h-16 rounded-full bg-gradient-to-br from-cyan-400 to-blue-600 shadow-2xl border-4 border-white flex items-center justify-center relative">
           <MessageCircle className="text-white" size={32} />
           {messages.length === 0 && (
             <div className="absolute top-0 right-0 w-4 h-4 bg-red-500 rounded-full border-2 border-white"></div>
           )}
        </div>
      </div>
    );
  }

  return (
    <div 
      ref={overlayRef}
      style={{ left: position.x, top: position.y }}
      className="fixed z-50 w-[300px] h-[400px] flex flex-col bg-white rounded-2xl shadow-2xl border border-gray-200 overflow-hidden font-sans"
    >
      <div 
        className="drag-handle bg-gradient-to-r from-cyan-500 to-blue-600 p-3 flex items-center justify-between cursor-move touch-none"
        onMouseDown={handlePointerDown}
        onTouchStart={handlePointerDown}
      >
        <div className="flex items-center gap-2 text-white font-bold">
          <MessageCircle size={18} />
          <span>Simple Rick</span>
        </div>
        <div className="flex gap-2 text-white/80">
          <button onClick={() => setIsExpanded(false)} className="hover:text-white"><Minimize2 size={18}/></button>
          <button onClick={onClose} className="hover:text-white"><X size={18}/></button>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 bg-slate-50 space-y-3">
        {messages.length === 0 && !isLoading ? (
            <div className="text-center text-gray-500 p-4">
                Welcome! Say something simple and cheerful!
            </div>
        ) : (
            messages.map((msg) => (
              <div key={msg.id} className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[80%] p-3 rounded-2xl text-sm ${
                  msg.sender === 'user' 
                    ? 'bg-blue-500 text-white rounded-br-none' 
                    : 'bg-white text-gray-800 shadow rounded-bl-none'
                }`}>
                  {msg.text}
                </div>
              </div>
            ))
        )}
        {isLoading && (
          <div className="flex justify-start">
            <div className="max-w-[80%] p-3 rounded-2xl text-sm bg-white text-gray-800 shadow rounded-bl-none flex items-center gap-2">
              <Loader2 size={16} className="animate-spin text-cyan-500"/>
              Rick is thinking simple thoughts...
            </div>
          </div>
        )}
        <div ref={chatEndRef} />
      </div>

      <div className="p-3 bg-white border-t space-y-2">
        <form onSubmit={sendMessage} className="flex gap-2">
          <input 
            type="text" 
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            placeholder={isLoading ? "Please wait..." : "Ask Rick..."}
            className="flex-1 bg-gray-100 rounded-full px-4 py-2 text-sm outline-none focus:ring-2 focus:ring-cyan-500 disabled:opacity-50"
            disabled={isLoading}
          />
          <button 
            type="submit" 
            className="bg-blue-600 text-white p-2 rounded-full hover:bg-blue-700 transition disabled:bg-gray-400"
            disabled={isLoading || !inputText.trim()}
          >
            <Send size={18} />
          </button>
        </form>
        <div className="text-[10px] text-gray-500 truncate text-center">
            User ID: {userId}
        </div>
      </div>
    </div>
  );
};

export default function App() {
  const [showOverlay, setShowOverlay] = useState(false);
  const [currentApp, setCurrentApp] = useState('home'); 
  
  const [db, setDb] = useState(null);
  const [userId, setUserId] = useState(null);
  const [isAuthReady, setIsAuthReady] = useState(false);

  useEffect(() => {
    if (Object.keys(firebaseConfig).length === 0) {
        console.error("Firebase config is missing. Running in local fallback mode.");
        setUserId(crypto.randomUUID()); 
        setIsAuthReady(true);
        return;
    }

    const app = initializeApp(firebaseConfig);
    const firestore = getFirestore(app);
    const firebaseAuth = getAuth(app);
    setDb(firestore);

    const authenticate = async () => {
        try {
            if (initialAuthToken) {
                await signInWithCustomToken(firebaseAuth, initialAuthToken);
            } else {
      
                await signInAnonymously(firebaseAuth);
            }
            console.log("Firebase authentication successful.");
        } catch (error) {
            console.error("Firebase authentication failed:", error);
        } finally {
            setIsAuthReady(true);
        }
    };

    authenticate();

    const unsubscribe = firebaseAuth.onAuthStateChanged(user => {
        if (user) {
            setUserId(user.uid);
        } else if (isAuthReady) {
            setUserId(crypto.randomUUID());
        }
    });

    return () => unsubscribe();
  }, []); 

  const handleLaunch = () => {
    setShowOverlay(true);
    setCurrentApp('browser'); 
  };

  const isLoaded = isAuthReady && db && userId;

  return (
    <div className="h-screen w-full bg-black flex justify-center items-center font-sans overflow-hidden">
      <div className="w-full max-w-md h-full max-h-[850px] bg-white relative shadow-2xl sm:rounded-[3rem] overflow-hidden border-[8px] border-gray-900 flex flex-col">
        
        <div className="bg-black text-white px-6 py-3 flex justify-between items-center text-xs z-50">
          <span>9:41</span>
          <div className="flex gap-2 items-center">
            <Signal size={14} />
            <Wifi size={14} />
            <Battery size={14} />
          </div>
        </div>

        <div className="flex-1 relative overflow-hidden">
          {currentApp === 'home' && <HomeScreen onLaunchOverlay={handleLaunch} />}
          {currentApp === 'browser' && <MockBrowser />}
          
          {showOverlay && isLoaded && (
            <Overlay 
                db={db} 
                userId={userId}
                onClose={() => {
                    setShowOverlay(false);
                    setCurrentApp('home');
            
