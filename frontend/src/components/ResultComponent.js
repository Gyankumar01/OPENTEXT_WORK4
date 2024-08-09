import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './ResultComponent.css';
import loaderGif from '../assets/loader.gif';

const ResultComponent = ({ jarInfos: initialJarInfos, onUpdate, filePath }) => {
    const [jarInfos, setJarInfos] = useState(initialJarInfos);
    const [selectedJars, setSelectedJars] = useState([]);
    const [statusMessage, setStatusMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [disabledDropdown, setDisabledDropdown] = useState({});
    const [showSuccessMessage, setShowSuccessMessage] = useState(false);
    const [updateComplete, setUpdateComplete] = useState(false);
    const [upgradeMode, setUpgradeMode] = useState(true); // true for Upgrade, false for Downgrade
    const [showUpgradeDowngrade, setShowUpgradeDowngrade] = useState(true); // New state for controlling visibility
    const [updatedJars, setUpdatedJars] = useState([]); // State to store updated dependencies

    useEffect(() => {
        const updateDisabledDropdowns = () => {
            const newDisabledDropdown = {};
            jarInfos.forEach(jar => {
                const olderVersions = getOlderVersions(jar.currentVersion, jar.availableVersions);
                const newerVersions = getNewerVersions(jar.currentVersion, jar.availableVersions);

                newDisabledDropdown[jar.artifact] = {
                    older: !olderVersions.length && !upgradeMode,
                    newer: !newerVersions.length && upgradeMode
                };
            });
            setDisabledDropdown(newDisabledDropdown);
        };

        const updatedJarInfos = jarInfos.map(jar => {
            const defaultNewVersion = getDefaultNewVersion(jar.availableVersions, jar.currentVersion);
            return {
                ...jar,
                newVersion: upgradeMode ? defaultNewVersion : '',
                olderVersion: !upgradeMode ? getOlderVersions(jar.currentVersion, jar.availableVersions)[0] : ''
            };
        });

        setJarInfos(updatedJarInfos);
        setSelectedJars([]);
        setShowSuccessMessage(false);
        setUpdateComplete(false);
        updateDisabledDropdowns();
        setShowUpgradeDowngrade(true); // Show upgrade/downgrade buttons when new file path is provided
    }, [initialJarInfos, upgradeMode, filePath]); // Added filePath to dependencies

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
        setUpdateComplete(false);

        // Prepare selectedJars to send with null values based on the mode
        const updatedSelectedJars = selectedJars.map(jar => ({
            ...jar,
            newVersion: upgradeMode ? jar.newVersion : null,
            olderVersion: upgradeMode ? null : jar.olderVersion
        }));

        try {
            await axios.post('http://localhost:9001/api/updateDependencies', updatedSelectedJars, {
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            setStatusMessage('Dependencies updated successfully');
            setShowSuccessMessage(true);
            setUpdateComplete(true);
            setShowUpgradeDowngrade(false); // Hide upgrade/downgrade buttons after update
            setUpdatedJars(updatedSelectedJars); // Store the updated dependencies
            onUpdate(selectedJars);
        } catch (error) {
            setStatusMessage('Failed to update dependencies: ' + error.message);
        } finally {
            setIsLoading(false);
        }
    };

    const handleVersionChange = (artifact, newVersion, type) => {
        if (newVersion === '') return;

        setJarInfos(prevInfos => prevInfos.map(jar => {
            if (jar.artifact === artifact) {
                if (type === 'older') {
                    return { ...jar, olderVersion: newVersion, newVersion: '' };
                } else {
                    return { ...jar, newVersion: newVersion, olderVersion: '' };
                }
            }
            return jar;
        }));

        setSelectedJars(prevSelected => {
            const selected = prevSelected.map(jar => {
                if (jar.artifact === artifact) {
                    if (type === 'older') {
                        return { ...jar, olderVersion: newVersion, newVersion: '' };
                    } else {
                        return { ...jar, newVersion: newVersion, olderVersion: '' };
                    }
                }
                return jar;
            });
            return selected.some(jar => jar.artifact === artifact)
                ? selected
                : [...selected, { ...jarInfos.find(jar => jar.artifact === artifact), [type === 'older' ? 'olderVersion' : 'newVersion']: newVersion }];
        });

        // Update disabled dropdowns
        const updatedDisabledDropdown = { ...disabledDropdown };
        if (type === 'older') {
            updatedDisabledDropdown[artifact] = {
                older: true,
                newer: updatedDisabledDropdown[artifact]?.newer || !upgradeMode
            };
        } else {
            updatedDisabledDropdown[artifact] = {
                older: updatedDisabledDropdown[artifact]?.older || !upgradeMode,
                newer: true
            };
        }
        setDisabledDropdown(updatedDisabledDropdown);
    };

    const getOlderVersions = (currentVersion, availableVersions) => {
        if (!currentVersion || !availableVersions) return [];
        return availableVersions.filter(version => version < currentVersion && version !== currentVersion).sort((a, b) => b.localeCompare(a, undefined, { numeric: true }));
    };

    const getNewerVersions = (currentVersion, availableVersions) => {
        if (!currentVersion || !availableVersions) return [];
        return availableVersions.filter(version => version > currentVersion && version !== currentVersion).sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
    };

    const getDefaultNewVersion = (availableVersions, currentVersion) => {
        if (!availableVersions || !Array.isArray(availableVersions) || !availableVersions.length) return '';
        return availableVersions.filter(version => version !== currentVersion).sort((a, b) => b.localeCompare(a, undefined, { numeric: true }))[0];
    };

    return (
        <div className="container">
            <div className="file-path">
                <p>File Path: {filePath}</p>
            </div>
            {showUpgradeDowngrade && ( // Conditionally render upgrade/downgrade radio buttons
                <div className="upgrade-downgrade">
                    <label>
                        <input
                            type="radio"
                            checked={upgradeMode}
                            onChange={() => setUpgradeMode(true)}
                        />
                        Upgrade
                    </label>
                    <label>
                        <input
                            type="radio"
                            checked={!upgradeMode}
                            onChange={() => setUpgradeMode(false)}
                        />
                        Downgrade
                    </label>
                </div>
            )}
            {showSuccessMessage ? (
                <div className="success-message">
                    <p>Selected jars are updated successfully!</p>
                    <h3>Updated Dependencies:</h3>
                    <ul>
                        {updatedJars.map(jar => (
                            <li key={jar.artifact}>
                                {jar.artifact}: {upgradeMode ? jar.newVersion : jar.olderVersion}
                            </li>
                        ))}
                    </ul>
                </div>
            ) : (
                <div className={`table-container ${updateComplete ? 'hidden' : ''}`}>
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
                                <th>Jar Name</th>
                                <th>Current Version</th>
                                <th>Older Version</th>
                                <th>Available Versions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {jarInfos.sort((a, b) => {
                                if (a.artifact.startsWith('com.emc') || a.artifact.startsWith('com.opentext')) return 1;
                                if (b.artifact.startsWith('com.emc') || b.artifact.startsWith('com.opentext')) return -1;
                                return 0;
                            }).map(jar => (
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
                                    <td>
                                        {getOlderVersions(jar.currentVersion, jar.availableVersions).length > 1 ? (
                                            <select
                                                value={jar.olderVersion || getOlderVersions(jar.currentVersion, jar.availableVersions)[0]}
                                                onChange={(e) => handleVersionChange(jar.artifact, e.target.value, 'older')}
                                                disabled={disabledDropdown[jar.artifact]?.older || upgradeMode}
                                            >
                                                <option value="">Select version</option>
                                                {getOlderVersions(jar.currentVersion, jar.availableVersions).map(version => (
                                                    <option key={version} value={version}>
                                                        {version}
                                                    </option>
                                                ))}
                                            </select>
                                        ) : getOlderVersions(jar.currentVersion, jar.availableVersions).length === 1 ? (
                                            <input
                                                type="text"
                                                value={getOlderVersions(jar.currentVersion, jar.availableVersions)[0]}
                                                disabled
                                            />
                                        ) : (
                                            <select disabled>
                                                <option value="">No Versions</option>
                                            </select>
                                        )}
                                    </td>
                                    <td>
                                        {getNewerVersions(jar.currentVersion, jar.availableVersions).length > 1 ? (
                                            <select
                                                value={jar.newVersion || getDefaultNewVersion(jar.availableVersions, jar.currentVersion)}
                                                onChange={(e) => handleVersionChange(jar.artifact, e.target.value, 'newer')}
                                                disabled={disabledDropdown[jar.artifact]?.newer || !upgradeMode}
                                            >
                                                {getNewerVersions(jar.currentVersion, jar.availableVersions).length ? (
                                                    <>
                                                        <option value="">{getDefaultNewVersion(jar.availableVersions, jar.currentVersion)}</option>
                                                        {getNewerVersions(jar.currentVersion, jar.availableVersions).map(version => (
                                                            <option key={version} value={version}>
                                                                {version}
                                                            </option>
                                                        ))}
                                                    </>
                                                ) : (
                                                    <option value="">No Versions</option>
                                                )}
                                            </select>
                                        ) : getNewerVersions(jar.currentVersion, jar.availableVersions).length === 1 ? (
                                            <input
                                                type="text"
                                                value={getNewerVersions(jar.currentVersion, jar.availableVersions)[0]}
                                                disabled
                                            />
                                        ) : (
                                            <select disabled>
                                                <option value="">No Versions</option>
                                            </select>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    <button onClick={handleUpdate} disabled={isLoading}>Update Selected JARs</button>
                    {isLoading && <img src={loaderGif} alt="Loading..." className="loader" />}
                    {statusMessage && <p className="status-message">{statusMessage}</p>}
                </div>
            )}
        </div>
    );
};
export default ResultComponent;