import React from 'react';

const ResultComponent = ({ jarInfos }) => {
    console.log(jarInfos); // Add this line for logging

    return (
        <div className="container">
            <h2>Third Party Library Upgrade</h2>
            <div className="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>JAR Name</th>
                            <th>Current Version</th>
                            <th>Newer Version</th>
                        </tr>
                    </thead>
                    <tbody>
                        {jarInfos.map((jar, index) => (
                            <tr key={index}>
                                <td>{jar.artifactId}</td>
                                <td>{jar.currentVersion}</td>
                                <td>{jar.newVersion}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default ResultComponent;
