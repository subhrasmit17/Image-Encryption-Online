async function encryptImage() {
    const isValid = await validateImageAndKey('encrypt');
    if (isValid) {
        await processImage('encrypt');
    }
}

async function decryptImage() {
    const isValid = await validateImageAndKey('decrypt');
    if (isValid) {
        await processImage('decrypt');
    }
}

//Validation Function
async function validateImageAndKey(action) {
    const fileInput = document.getElementById('imageInput');
    const keyInput = document.getElementById('keyInput');

    if (fileInput.files.length === 0 || keyInput.value === '') {
        alert('Please select an image and enter a key.');
        return false;
    }

    const file = fileInput.files[0];
    const maxFileSize = 10 * 1024 * 1024; // 1.5 MB
    const allowedTypes = ['image/png', 'image/jpeg'];
    const minKeyLength = 1;
    const maxKeyLength = 15;
    const maxTotalPixels = 100000000000000000000; //1000000

    // File size validation(ignore for decryption)
    if (action === 'encrypt' && file.size > maxFileSize) {
        alert('File size exceeds 1.5MB limit. Please upload a smaller image.');
        return false;
    }

    // File type validation
    if (!allowedTypes.includes(file.type)) {
        alert('Unsupported file type. Please upload a PNG or JPG image.');
        return false;
    }

    // Key size validation
    const keyLength = keyInput.value.length;
    if (keyLength < minKeyLength || keyLength > maxKeyLength) {
        alert('Key must be shorter than 15 characters.');
        return false;
    }

    // Image resolution (total pixel count) validation
    const isValidResolution = await validateImageResolution(file, maxTotalPixels);
    if (!isValidResolution) {
        return false;
    }

    //All validations passed
    return true;
}

//Image Resolution Validator
function validateImageResolution(file, maxTotalPixels) {
    return new Promise((resolve) => {
        const img = new Image();
        img.onload = function () {
            const totalPixels = img.width * img.height;
            if (totalPixels > maxTotalPixels) {
                alert(`Image resolution exceeds limit. Maximum allowed total pixels: ${maxTotalPixels}. Please upload a smaller image.`);
                resolve(false);
            } else {
                resolve(true);
            }
        };
        img.onerror = function () {
            alert('Error loading the image. Please upload a valid image file.');
            resolve(false);
        };
        img.src = URL.createObjectURL(file);
    });
}

//Process Image Function
async function processImage(action) {
    const fileInput = document.getElementById('imageInput');
    const keyInput = document.getElementById('keyInput');

    const loader = document.getElementById('loader');

    const encryptButton = document.querySelector('button[onclick="encryptImage()"]');
    const decryptButton = document.querySelector('button[onclick="decryptImage()"]');

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    formData.append('key', keyInput.value);

    try {
        //disable buttons while processing
        encryptButton.disabled = true;
        decryptButton.disabled = true;

        //show loader
        loader.style.display = 'block';

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
        console.error('Request failed:', error);

        let userFriendlyMessage = mapErrorToUserMessage(error.message);
        alert(userFriendlyMessage);
    } finally {
        //enable buttons once process is complete
        encryptButton.disabled = false;
        decryptButton.disabled = false;
        
        //hide loader
        loader.style.display = 'none';
    }
}

//Error Mapping Function
function mapErrorToUserMessage(errorMessage) {
    if (errorMessage.includes('OutOfMemoryError')) {
        return 'Unable to process image due to insufficient memory in server. Please try uploading a smaller image.';
    } else if (errorMessage.includes('ServletException')) {
        return 'The server encountered a problem processing your request.';
    } else if (errorMessage.includes('Internal Server Error')) {
        return 'An unexpected error occurred on the server. Please try again later.';
    } else if (errorMessage.includes('NetworkError')) {
        return 'Failed to connect to the server. Please check your internet connection.';
    } else {
        return 'Request failed: ' + errorMessage;
    }
}