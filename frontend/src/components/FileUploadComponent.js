import React, { useState } from 'react';
import axios from 'axios';
import ResultComponent from './ResultComponent';
import './FileUploadComponent.css';

function FileUploadComponent() {
    const [filePath, setFilePath] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [jarInfos, setJarInfos] = useState([]);

    const handleUpload = () => {
        setIsLoading(true);
        axios.post('http://localhost:9001/api/uploadFilePath', { filePath })
            .then(response => {
                console.log('Response data:', response.data);
                setJarInfos(response.data);
                setIsLoading(false);
            })
            .catch(error => {
                console.error('There was an error uploading the file!', error);
                setIsLoading(false);
            });
    };

    const handleUpdate = (updatedJars) => {
        // Handle the updated JARs
        console.log('Updated JARs:', updatedJars);
        // Optionally, you can re-fetch the jarInfos to get the latest state
        // fetchJarInfos();
    };

    return (
        <div className="file-upload-container">
            <h1>Third Party Library Upgrade</h1>
            <div className="file-upload">
                <input
                    type="text"
                    value={filePath}
                    onChange={e => setFilePath(e.target.value)}
                    placeholder="Enter file path"
                />
                <button onClick={handleUpload} disabled={isLoading}>Upload</button>
            </div>
            {isLoading && <div className="loader"></div>}
            {!isLoading && jarInfos.length > 0 && (
                <ResultComponent jarInfos={jarInfos} onUpdate={handleUpdate} filePath={filePath} />
            )}
        </div>
    );
}

export default FileUploadComponent;
