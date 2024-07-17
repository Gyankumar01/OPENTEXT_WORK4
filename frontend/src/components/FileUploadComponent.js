import React, { useState } from 'react';
import axios from 'axios';
import './FileUploadComponent.css';

const FileUploadComponent = ({ onUpload }) => {
    const [filePath, setFilePath] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setFilePath(file.path || file.webkitRelativePath || file.mozFullPath || file.name);
        }
    };

    const handleUpload = async () => {
        if (!filePath) return;
        setIsLoading(true);
        try {
            const response = await axios.post('http://localhost:9001/api/upload', { filePath }, {
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            onUpload(response.data);
        } catch (error) {
            console.error('Failed to upload the file:', error);
            onUpload([]);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="container">
            <h2 className="header">Third Party Library Upgrade</h2>
            <button className="upload-button" onClick={handleUpload} disabled={isLoading}>Upload</button>
            <label htmlFor="file-upload" className="choose-file-button">Choose File</label>
            <input type="file" id="file-upload" className="file-input" onChange={handleFileChange} />
            {filePath && <p className="file-name">Selected File: {filePath}</p>}
            {isLoading && <div className="spinner"></div>}
        </div>
    );
};

export default FileUploadComponent;
