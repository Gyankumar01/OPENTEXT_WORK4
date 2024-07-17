import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './ResultComponent.css';

const ResultComponent = ({ jarInfos, onUpdate }) => {
    const [selectedJars, setSelectedJars] = useState([]);
    const [statusMessage, setStatusMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        setSelectedJars([]);
    }, [jarInfos]);

    const handleSelectJar = (jar) => {
        setSelectedJars(prevSelected =>
            prevSelected.some(item => item.artifactId === jar.artifactId)
                ? prevSelected.filter(item => item.artifactId !== jar.artifactId)
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
            const response = await axios.post('http://localhost:9001/api/update', selectedJars, {
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
                            <th>Property</th>
                            <th>Current Version</th>
                            <th>Newer Version</th>
                        </tr>
                    </thead>
                    <tbody>
                        {jarInfos.map((jar) => (
                            <tr key={jar.artifactId}>
                                <td>
                                    <input
                                        type="checkbox"
                                        checked={selectedJars.some(item => item.artifactId === jar.artifactId)}
                                        onChange={() => handleSelectJar(jar)}
                                    />
                                </td>
                                <td>{jar.artifactId}</td>
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
