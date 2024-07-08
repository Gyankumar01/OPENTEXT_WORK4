import React, { useState } from 'react';
import './App.css';
import FileUploadComponent from './components/FileUploadComponent';
import ResultComponent from './components/ResultComponent';

function App() {
    const [jarInfos, setJarInfos] = useState([]);

    const handleUpload = (data) => {
        setJarInfos(data);
    };

    return (
        <div className="App">
            <FileUploadComponent onUpload={handleUpload} />
            {jarInfos.length > 0 && <ResultComponent jarInfos={jarInfos} />}
        </div>
    );
}

export default App;
