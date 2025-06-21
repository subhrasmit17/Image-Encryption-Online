async function encryptImage() {
    await processImage('encrypt');
}

async function decryptImage() {
    await processImage('decrypt');
}

async function processImage(action) {
    const fileInput = document.getElementById('imageInput');
    const keyInput = document.getElementById('keyInput');

    if (fileInput.files.length === 0 || keyInput.value === '') {   //pressed encrypt/decrypt button without adding image or key
        alert('Please select an image and enter a key.');
        return;
    }

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    formData.append('key', keyInput.value);

    try {
        const response = await fetch(`https://image-encryptor-backend.onrender.com/api/image/${action}?key=${keyInput.value}`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            let errorText = '';
            try {
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    const errorData = await response.json();
                    errorText = errorData.error || JSON.stringify(errorData);
                } else {
                    errorText = await response.text();
                }
            } catch (parseError) {
                errorText = 'Unknown error occurred';
            }
            throw new Error(errorText);
        }

        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = (action === 'encrypt' ? 'encrypted.png' : 'decrypted.png');
        document.body.appendChild(a);
        a.click();
        a.remove();
    } catch (error) {
        alert('Request failed: ' + error.message);
    }
}