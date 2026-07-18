import { useState } from 'react';
import Editor from '@monaco-editor/react';
import axios from 'axios';

function App() {
  const [code, setCode] = useState('print("Hello from the Web IDE!")');
  const [output, setOutput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  // State to track which language the user has selected
  const [language, setLanguage] = useState('python');

  const handleRunCode = async () => {
    setIsLoading(true);
    setOutput('Executing code in secure sandbox...\n');
    
    try {
      const response = await axios.post('/api/execute', {
        language: language,
        code: code
      });
      setOutput(response.data.output);
    } catch (error) {
      setOutput('Error connecting to the execution engine:\n' + error.message);
    } finally {
      setIsLoading(false);
    }
  };

  // Automatically switch the starter code when the user changes the language
  const handleLanguageChange = (e) => {
    const newLang = e.target.value;
    setLanguage(newLang);
    if (newLang === 'python') {
      setCode('print("Hello from the Web IDE!")');
    } else if (newLang === 'cpp') {
      setCode('#include <iostream>\n\nint main() {\n    std::cout << "Hello from C++!" << std::endl;\n    return 0;\n}');
    }
  };

  return (
    // Outer Shell
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', backgroundColor: '#0d1117', color: '#fff', fontFamily: 'sans-serif' }}>
      
      {/* Top Navigation */}
      <div style={{ padding: '12px 20px', backgroundColor: '#010409', borderBottom: '1px solid #30363d', display: 'flex', alignItems: 'center' }}>
        {/* Fake decorative "server status" dot */}
        <div style={{ width: '10px', height: '10px', backgroundColor: '#3fb950', borderRadius: '50%', marginRight: '12px', boxShadow: '0 0 8px #3fb950' }}></div>
        <h2 style={{ fontSize: '13px', fontWeight: '600', margin: 0, color: '#c9d1d9', letterSpacing: '1px' }}>
          DISTRIBUTED RCE ENGINE
        </h2>
      </div>

      {/* Workspace Flexbox */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        
        {/* Left Side: Editor Pane */}
        <div style={{ flex: '6', display: 'flex', flexDirection: 'column', borderRight: '1px solid #30363d' }}>
          
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 16px', backgroundColor: '#161b22', borderBottom: '1px solid #30363d' }}>
            
            {/* Dynamic File Name */}
            <div style={{ fontSize: '13px', color: '#8b949e', fontFamily: 'monospace' }}>
              {language === 'python' ? 'main.py' : 'main.cpp'}
            </div>
            
            {/* Control Group (Dropdown + Run Button) */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              
              {/* Dropdown */}
              <select 
                value={language} 
                onChange={handleLanguageChange}
                style={{ backgroundColor: '#21262d', color: '#c9d1d9', border: '1px solid #30363d', padding: '5px 10px', borderRadius: '6px', fontSize: '12px', outline: 'none', cursor: 'pointer' }}
              >
                <option value="python">Python 3.9</option>
                <option value="cpp">C++ (GCC)</option>
              </select>

              {/* Run Button */}
              <button
                onClick={handleRunCode}
                disabled={isLoading}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '5px 16px', 
                  backgroundColor: isLoading ? '#23863680' : '#238636',
                  color: '#ffffff', 
                  border: '1px solid rgba(240, 246, 252, 0.1)', 
                  borderRadius: '6px', 
                  cursor: isLoading ? 'not-allowed' : 'pointer', 
                  fontWeight: '600',
                  fontSize: '12px',
                  transition: 'background-color 0.2s',
                }}
              >
                {/* SVG Play Icon */}
                {!isLoading && (
                  <svg width="12" height="12" viewBox="0 0 16 16" fill="currentColor">
                    <path d="M11.596 8.697l-6.363 3.692c-.54.313-1.233-.066-1.233-.697V4.308c0-.63.692-1.01 1.233-.696l6.363 3.692a.802.802 0 0 1 0 1.393z"></path>
                  </svg>
                )}
                {isLoading ? 'EXECUTING...' : 'RUN'}
              </button>
            </div>
          </div>

          {/* Editor */}
          <div style={{ flex: 1, backgroundColor: '#0d1117' }}>
            <Editor
              height="100%"
              language={language === 'cpp' ? 'cpp' : 'python'}
              theme="vs-dark"
              value={code}
              onChange={(value) => setCode(value)}
              options={{ 
                minimap: { enabled: false },
                fontSize: 14,
                padding: { top: 16 },
                scrollBeyondLastLine: false
              }}
            />
          </div>
        </div>

        {/* Right Side: Terminal Pane */}
        <div style={{ flex: '4', display: 'flex', flexDirection: 'column', backgroundColor: '#010409' }}>
           {/* Terminal Header */}
           <div style={{ padding: '9px 16px', backgroundColor: '#161b22', fontSize: '12px', fontWeight: '600', color: '#8b949e', borderBottom: '1px solid #30363d', letterSpacing: '0.5px', textTransform: 'uppercase' }}>
            Terminal Output
          </div>
          {/* Terminal Body */}
          <div style={{ flex: 1, padding: '16px', overflowY: 'auto', fontFamily: 'monospace', fontSize: '13px' }}>
            <pre style={{ color: '#3fb950', margin: 0, whiteSpace: 'pre-wrap', lineHeight: '1.5' }}>
              {output || '> Engine ready. Waiting for payload...'}
            </pre>
          </div>
        </div>

      </div>
    </div>
  );
}

export default App;