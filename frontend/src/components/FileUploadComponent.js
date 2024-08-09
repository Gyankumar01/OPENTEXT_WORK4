import React, { useState } from 'react';
import axios from 'axios';
import ResultComponent from './ResultComponent';
import './FileUploadComponent.css';
import d2Logo from '../assets/d2logo.png'; // Adjust the path as needed
import loaderGif from '../assets/loader.gif'; // Path to the new loader GIF

function FileUploadComponent() {
    const [filePath, setFilePath] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [jarInfos, setJarInfos] = useState([]);

    const handleUpload = () => {
        setIsLoading(true);
        axios.post('http://localhost:9001/api/uploadFilePath', { filePath })
            .then(response => {
                console.log('Response data:', response.data);
                setJarInfos(response.data); // Set jarInfos from response
                setIsLoading(false);
            })
            .catch(error => {
                console.error('There was an error uploading the file!', error);
                setIsLoading(false);
            });
    };

    const handleUpdate = (updatedJars) => {
        console.log('Updated JARs:', updatedJars);
    };

    return (
        <div className="file-upload-container">
            <header className="header">
                <a href="https://mvnrepository.com/" className="mvn-repo-button" target="_blank" rel="noopener noreferrer">
                    MVN REPO
                </a>
                <h1>Third Party Library Upgrade</h1>
                <img src={d2Logo} alt="Header Logo" className="header-logo" />
            </header>
            <div className="file-upload">
                <input
                    type="text"
                    value={filePath}
                    onChange={e => setFilePath(e.target.value)}
                    placeholder="Enter address of directory in filesystem"
                />
                <button onClick={handleUpload} disabled={isLoading}>Process Path</button>
            </div>
            {isLoading && <img src={loaderGif} alt="Loading..." className="loader" />}
            {!isLoading && jarInfos.length > 0 && (
                <ResultComponent jarInfos={jarInfos} onUpdate={handleUpdate} filePath={filePath} />
            )}
            <footer className="footer">
                <p>Copyright © 2024 Open Text. All rights reserved. Trademarks owned by Open Text.</p>
                <p>One or more patents may cover this product. For more information, please visit <a href="https://www.opentext.com/patents" target="_blank" rel="noopener noreferrer">OpenText Patents</a>.</p>
            </footer>
        </div>
    );
}

export default FileUploadComponent;
