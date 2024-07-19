import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './ResultComponent.css';

const ResultComponent = ({ jarInfos, onUpdate, filePath }) => {
    const [selectedJars, setSelectedJars] = useState([]);
    const [statusMessage, setStatusMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        setSelectedJars([]);
    }, [jarInfos]);

    const handleSelectJar = (jar) => {
        setSelectedJars(prevSelected =>
            prevSelected.some(item => item.artifact === jar.artifact)
                ? prevSelected.filter(item => item.artifact !== jar.artifact)
                : [...prevSelected, jar]
        );
    };

    const handleSelectAll = () => {
        if (selectedJars.length === jarInfos.length) {
            setSelectedJars([]);
        } else {
            setSelectedJars(jarInfos);
        }
    };

    const handleUpdate = async () => {
        setIsLoading(true);
        setStatusMessage('');
        try {
            const response = await axios.post('http://localhost:9001/api/updateDependencies', selectedJars, {
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            setStatusMessage('Dependencies updated successfully');
            onUpdate(selectedJars);
        } catch (error) {
            setStatusMessage('Failed to update dependencies: ' + error.message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="container">
            <h2>Third Party Library Upgrade</h2>
            <div className="file-path">
                <p>File Path: {filePath}</p>
            </div>
            <div className="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>
                                <input
                                    type="checkbox"
                                    checked={selectedJars.length === jarInfos.length}
                                    onChange={handleSelectAll}
                                />
                            </th>
                            <th>Artifact</th>
                            <th>Current Version</th>
                            <th>New Version</th>
                        </tr>
                    </thead>
                    <tbody>
                        {jarInfos.map((jar) => (
                            <tr key={jar.artifact}>
                                <td>
                                    <input
                                        type="checkbox"
                                        checked={selectedJars.some(item => item.artifact === jar.artifact)}
                                        onChange={() => handleSelectJar(jar)}
                                    />
                                </td>
                                <td>{jar.artifact}</td>
                                <td>{jar.currentVersion}</td>
                                <td>{jar.newVersion}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                <button className="update-button" onClick={handleUpdate} disabled={isLoading || selectedJars.length === 0}>
                    {isLoading ? 'Updating...' : 'Update'}
                </button>
            </div>
            {isLoading && <div className="loader"></div>}
            {statusMessage && <div className="status-message">{statusMessage}</div>}
        </div>
    );
};

export default ResultComponent;
