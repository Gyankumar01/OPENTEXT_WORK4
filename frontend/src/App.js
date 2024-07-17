import React, { useState } from 'react';
import './App.css';
import FileUploadComponent from './components/FileUploadComponent';
import ResultComponent from './components/ResultComponent';
import axios from 'axios';

function App() {
    const [jarInfos, setJarInfos] = useState([]);

    const handleUpload = (data) => {
        setJarInfos(data);
    };

    const handleUpdate = async (selectedJars) => {
        try {
            await axios.post('http://localhost:9001/api/update', selectedJars, {
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            // Handle successful update
        } catch (error) {
            console.error('Failed to update dependencies:', error);
            // Handle error state
        }
    };

    return (
        <div className="App">
            <FileUploadComponent onUpload={handleUpload} />
            {jarInfos.length > 0 && <ResultComponent jarInfos={jarInfos} onUpdate={handleUpdate} />}
        </div>
    );
}

export default App;
