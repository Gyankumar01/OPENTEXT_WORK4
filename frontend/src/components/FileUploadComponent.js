import React, { useState } from 'react';
import axios from 'axios';

const FileUploadComponent = ({ onUpload }) => {
    const [file, setFile] = useState(null);

    const handleFileChange = (e) => {
        setFile(e.target.files[0]);
    };

    const handleUpload = async () => {
        if (!file) return;

        try {
            const formData = new FormData();
            formData.append('file', file);

            const response = await axios.post('http://localhost:9001/api/upload', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            });

            onUpload(response.data); // Pass data to parent component
        } catch (error) {
            console.error('Failed to upload the file:', error);
            onUpload([]); // Handle error state
        }
    };

    return (
        <div>
            <input type="file" onChange={handleFileChange} />
            <button onClick={handleUpload}>Upload</button>
        </div>
    );
};

export default FileUploadComponent;
