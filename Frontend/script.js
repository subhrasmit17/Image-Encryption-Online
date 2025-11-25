let lastEncryptedBlob = null;

document.getElementById("imageInput").addEventListener("change", () => {
    document.getElementById("metricsCard").style.display = "none";
    document.getElementById("npcrValue").innerText = "--";

    document.getElementById("previewOutputWrapper").style.display = "none";
    document.getElementById("downloadButton").style.display = "none";

    const file = document.getElementById("imageInput").files[0];
    if (!file) return;

    const url = URL.createObjectURL(file);
    document.getElementById("previewUploaded").src = url;
    document.getElementById("previewUploadedWrapper").style.display = "block";
});

async function encryptImage() {
    if (await validateImageAndKey("encrypt")) {
        await processImage("encrypt");
    }
}

async function decryptImage() {
    if (await validateImageAndKey("decrypt")) {
        await processImage("decrypt");
    }
}

async function processImage(action) {
    const fileInput = document.getElementById("imageInput");
    const keyInput = document.getElementById("keyInput");
    const loader = document.getElementById("loader");

    const encryptButton = document.querySelector('button[onclick="encryptImage()"]');
    const decryptButton = document.querySelector('button[onclick="decryptImage()"]');

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    formData.append('key', keyInput.value);

    try {
        encryptButton.disabled = true;
        decryptButton.disabled = true;
        loader.style.display = 'block';

        const response = await fetch(
            `https://image-encryptor-backend.onrender.com/api/image/${action}?key=${keyInput.value}`,
            { method: "POST", body: formData }
        );

        if (!response.ok) throw new Error(await response.text());

        const blob = await response.blob();
        lastEncryptedBlob = blob;

        const url = URL.createObjectURL(blob);
        const previewOutput = document.getElementById("previewOutput");

        previewOutput.src = url;
        document.getElementById("previewOutputWrapper").style.display = "block";
        document.getElementById("downloadButton").style.display = "block";

        if (action === "encrypt") {
            await calculateNPCR(fileInput.files[0], blob);
        }

    } catch (e) {
        alert("Processing failed. " + e.message);
    } finally {
        encryptButton.disabled = false;
        decryptButton.disabled = false;
        loader.style.display = 'none';
    }
}

function downloadEncryptedImage() {
    if (!lastEncryptedBlob) return;
    const a = document.createElement("a");
    a.href = URL.createObjectURL(lastEncryptedBlob);
    a.download = "encrypted.png";
    a.click();
}
