/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

(function() {
    const jsonType = 'application/json;charset=utf-8';
    const statusText = document.querySelector('.ma-startup-text');
    const progressText = document.querySelector('.ma-startup-progress');
    
    const xhrRequest = (options) => {
        const xhr = new XMLHttpRequest();
        
        return new Promise((resolve, reject) => {
            xhr.addEventListener('load', resolve);
            xhr.addEventListener('error', reject);
            xhr.addEventListener('abort', reject);
            xhr.addEventListener('timeout', reject);
            
            xhr.open(options.method || 'GET', options.url);
            xhr.timeout = options.timeout != null ? options.timeout : 30000;
            
            const headers = options.headers || {};
            const seenHeaders = new Set();
            for (let [name, value] of Object.entries(headers)) {
                seenHeaders.add(name.toLowerCase());
                xhr.setRequestHeader(name, value);
            }
            
            if (!seenHeaders.has('accept')) {
                xhr.setRequestHeader('accept', jsonType);
            }

            if (options.data) {
                if (!seenHeaders.has('content-type')) {
                    xhr.setRequestHeader('content-type', jsonType);
                }
                const encode = options.encode || JSON.stringify;
                xhr.send(encode(options.data));
            } else {
                xhr.send();
            }
        }).then(event => {
            const contentTypeHeader = xhr.getResponseHeader('content-type');
            const contentType = contentTypeHeader && contentTypeHeader.split(';')[0];
            
            let data;
            if (contentType && contentType.split(';')[0] === 'application/json' && xhr.responseText) {
                try {
                    data = JSON.parse(xhr.responseText);
                } catch (e) {}
            }
            
            return {
                status: xhr.status,
                statusText: xhr.statusText,
                getHeader: xhr.getResponseHeader.bind(xhr),
                data,
                xhr
            };
        });
    };
    
    const setMessage = (message, progress) => {
        statusText.textContent = message;
        document.title = `${progress}% - ${message}`;
        statusText.classList.remove('ma-startup-error');
        progressText.style.display = 'block';
        progressText.textContent = `${progress}%`;
    };
    
    const setErrorMessage = (message) => {
        statusText.textContent = `Error getting status: ${message}`;
        document.title = message;
        statusText.classList.add('ma-startup-error');
        progressText.style.display = 'none';
    };
    
    const fetchStatus = () => {
        return xhrRequest({
            url: '/status',
            timeout: 10000
        }).then(response => {
            if (response.status == 200 && response.data) {
                setMessage(response.data.state, response.data.startupProgress);
                
                const path = window.location.pathname;
                if (response.data.stateName === 'RUNNING' && path !== '/startup/' && path !== '/startup/index.html') {
                    window.location.reload();
                }
            } else {
                setErrorMessage(`HTTP error ${response.status} \u2014 ${response.statusText}`);
            }
        }, error => {
            if (window.navigator.onLine === false) {
                setErrorMessage('You are offline');
            } else {
                setErrorMessage('Server not responding');
            }
        });
    };

    const fetchStatusFixedDelay = (delay) => {
        const doAgain = () => {
            setTimeout(() => fetchStatusFixedDelay(delay), delay);
        };
        fetchStatus().then(doAgain, doAgain);
    };

    fetchStatusFixedDelay(5000);
})();
