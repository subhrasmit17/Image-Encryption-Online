let lastEncryptedBlob = null;


document.getElementById("imageInput").addEventListener("change", () => {
    // hide psnr
    document.getElementById("metricsCard").style.display = "none";
    document.getElementById("npcrValue").innerText = "--";

    // hide old output
    document.getElementById("previewOutputWrapper").style.display = "none";
    document.getElementById("downloadButton").style.display = "none";

    const file = document.getElementById("imageInput").files[0];
    if (!file) return;

    const url = URL.createObjectURL(file);
    const previewImg = document.getElementById("previewUploaded");
    const previewWrapper = document.getElementById("previewUploadedWrapper");

    previewImg.src = url;
    previewWrapper.style.display = "block";

    // Set dynamic aspect ratio
    updatePreviewAspectRatio(previewImg, previewWrapper);
});


function updatePreviewAspectRatio(imgElement, wrapperElement) {
    const img = imgElement;

    // Wait for image to load
    img.onload = () => {
        const w = img.naturalWidth;
        const h = img.naturalHeight;

        if (w && h) {
            const ratio = w / h;
            wrapperElement.style.aspectRatio = `${ratio}`;
        }
    };
}








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
    const maxFileSize = 1.5 * 1024 * 1024; // 1.5 MB
    const allowedTypes = ['image/png', 'image/jpeg'];
    const minKeyLength = 1;
    const maxKeyLength = 15;
    const maxTotalPixels = 1000000;

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

        // Save encrypted blob for download
        lastEncryptedBlob = blob;




        // Create preview URL
        const downloadUrl = URL.createObjectURL(blob);

        // Show encrypted image preview
        const previewOutput = document.getElementById("previewOutput");
        const previewWrapper2 = document.getElementById("previewOutputWrapper");

        previewOutput.src = downloadUrl;
        previewWrapper2.style.display = "block";
        previewOutput.style.display = "block";

        // Dynamic aspect ratio for encrypted image
        updatePreviewAspectRatio(previewOutput, previewWrapper2);


        // Show download button
        document.getElementById("downloadButton").style.display = "block";

        // Calculate NPCR only after encryption
        if (action === "encrypt") {
            const originalBlob = fileInput.files[0];
            await calculateNPCR(originalBlob, blob);
        }


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

async function calculateNPCR(originalBlob, encryptedBlob) {
    const formData = new FormData();
    formData.append("image1", originalBlob);
    formData.append("image2", encryptedBlob);

    try {
        const response = await fetch("https://image-encryptor-backend.onrender.com/api/image/npcr", {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            throw new Error(await response.text());
        }

        const data = await response.json();

        // Update UI
        document.getElementById("npcrValue").innerText = data.NPCR.toFixed(4);
        document.getElementById("metricsCard").style.display = "block";

    } catch (err) {
        console.error("NPCR calculation error:", err);
        alert("Failed to calculate NPCR.");
    }
}



function downloadEncryptedImage() {
    if (!lastEncryptedBlob) {
        alert("No encrypted image available to download.");
        return;
    }

    const url = URL.createObjectURL(lastEncryptedBlob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "encrypted.png";
    document.body.appendChild(a);
    a.click();
    a.remove();
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