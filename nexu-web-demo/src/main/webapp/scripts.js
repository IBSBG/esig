function checkAgentStatus(lang) {
    document.getElementById("messageUnsuccessfulConnection").style.display = "block";
    document.getElementById("messageSuccessfulConnection").style.display = "none";
    document.getElementById("submitButton").disabled = true;
    let request = new XMLHttpRequest();
    request.open("GET", "http://localhost:9795/nexu-info", true);
    request.send();
    request.onload = () => {
        if (request.status == 200) {
            let responseText = JSON.parse(request.response).version;
            console.log(JSON.parse(request.response).version);
            if ("1.23-SNAPSHOT" === responseText) {
                document.getElementById("messageUnsuccessfulConnection").style.display = "none";
                document.getElementById("messageSuccessfulConnection").style.display = "block";
                document.getElementById("submitButton").disabled = false;
            }
        } else {
            alert("ERROR status=" + request.status + " statusText=" + request.statusText);
        }
    }
    request.onerror = function (e) {
        document.getElementById("messageUnsuccessfulConnection").style.display = "block";
        document.getElementById("messageSuccessfulConnection").style.display = "none";
        document.getElementById("submitButton").disabled = true;
    };
}

// <div className="message alert-danger"><strong>Неуспешно свързване с NexU локална компонента! Моля, изтеглете инсталационните файлове и следвайте инструкциите.</strong></div>
// <div className="message alert-success"><strong>Успешно свързване с NexU локална компонента!</strong></div>

function sign(requestParameters) {
    let request = new XMLHttpRequest();
    request.open("POST", "http://localhost:9795/rest/signDoc", true);
    request.send(JSON.stringify(requestParameters));
    request.onload = () => {
        if (request.status == 200) {
            console.log(request.response)

            json = JSON.parse(request.response);

            if (json.response.success == true) {
                const binaryImg = atob(json.response.signedFileBase64);
                const length = binaryImg.length;
                const arrayBuffer = new ArrayBuffer(length);
                const uintArray = new Uint8Array(arrayBuffer);

                for (let i = 0; i < length; i++) {
                    uintArray[i] = binaryImg.charCodeAt(i);
                }

                const fileBlob = new Blob([uintArray], {type: 'application/pdf'});

                // console.log(json.response.signedFileBase64);

                // let data = { x: 42, s: file, d: new Date() };
                saveData(fileBlob, json.response.signedFileName);
            }
        } else {
            let message = "ERROR status=" + request.status + " statusText=" + request.statusText + "\n";
            json = JSON.parse(request.response);
            if (json.error != undefined) message += json.error;
            alert(message);

        }
    }
}

let saveData = (function () {
    let a = document.createElement("a");
    document.body.appendChild(a);
    a.style = "display: none";
    return function (file, fileName) {
        // let json = JSON.stringify(file);
        // console.log(file);
        // let blob = new Blob([file], {type: "octet/stream"}),
        let blob = new Blob([file], {type: "application/octet-stream"}),
            url = window.URL.createObjectURL(blob);
        a.href = url;
        a.download = fileName;
        a.click();
        window.URL.revokeObjectURL(url);
        resetForm()
    };
}());

function resetForm() {
    document.getElementById("signatureFormatPAdES").checked = false;
    document.getElementById("signatureFormatCAdES").checked = false;
    document.getElementById("signatureFormatXAdES").checked = false;

    document.getElementById("packagingFormatEnveloped").disabled = true;
    document.getElementById("packagingFormatEnveloped").checked = false;
    document.getElementById("packagingFormatDetached").disabled = true;
    document.getElementById("packagingFormatDetached").checked = false;
    document.getElementById("packagingFormatEnveloping").disabled = true;
    document.getElementById("packagingFormatEnveloping").checked = false;
    document.getElementById("packagingFormatInternallyDetached").disabled = true;
    document.getElementById("packagingFormatInternallyDetached").checked = false;

    document.getElementById("signatureLevelBASELINE_B").checked = false;
    document.getElementById("signatureLevelBASELINE_T").checked = false;
    document.getElementById("signatureLevelBASELINE_LT").checked = false;
    document.getElementById("signatureLevelBASELINE_LTA").checked = false;
}

document.addEventListener('submit', function (event) {

    // Prevent form from submitting to the server
    event.preventDefault();

    let formData = new FormData(event.target);
    let file = formData.get('file');
    let container = formData.get('container');
    let signatureFormat = formData.get('signatureFormat');
    let packagingFormat = formData.get('packagingFormat');
    let signatureLevel = formData.get('signatureLevel');
    let digestAlgorithm = formData.get('digestAlgorithm');

    if(file.name === ""){
        alert("Моля, изберете файл за подписване!");
        document.getElementById("file").style.border = "1px solid red";
        return;
    }
    document.getElementById("file").style.border = "none";

    if (signatureFormat === null) {
        if ("application/pdf" === file.type) {
            signatureFormat = "pades";
            document.getElementById("signatureFormatPAdES").checked = true;
        } else if ("text/xml" === file.type) {
            signatureFormat = "xades"
            document.getElementById("signatureFormatXAdES").checked = true;
        }
    }
    if (packagingFormat === null) {
        document.getElementById("packagingFormatEnveloped").disabled = false;
        document.getElementById("packagingFormatEnveloped").checked = true;
        packagingFormat = "enveloped"
    }
    if (signatureLevel === null) {
        if ("application/pdf" === file.type) {
            signatureLevel = "PAdES-BASELINE-B";
            document.getElementById("signatureLevelBASELINE_B").checked = true;
        } else if ("text/xml" === file.type) {
            signatureLevel = "XAdES-BASELINE-B"
            document.getElementById("signatureLevelBASELINE_B").checked = true;
        }
    }

    // alert(" Файл [" + file.name + "]" +
    //     "\n Размер на файла [" + file.size + " bytes]" +
    //     "\n Контейнер [" + container + "]" +
    //     "\n Формат на подписа [" + signatureFormat + "]" +
    //     "\n Тип на подписа [" + packagingFormat + "]" +
    //     "\n Ниво на подписa [" + signatureLevel + "]" +
    //     "\n Хеш алгоритъм [" + digestAlgorithm + "]"
    // );

    let reader = new FileReader();
    reader.onloadend = function () {
        let arrayBuffer = this.result;
        var fileBase64Format = arrayBuffer.split(',')[1];
        // alert(arrayBuffer)
        // let fileByteArray = new Uint8Array(arrayBuffer);
        // let binaryString = String.fromCharCode.apply(null, fileByteArray);
        // alert("Файл byte array [" + fileByteArray + "]");

        let requestParameters = {
            container: container,
            signatureFormat: signatureFormat,
            packagingFormat: packagingFormat,
            signatureLevel: signatureLevel,
            digestAlgorithm: digestAlgorithm,
            fileBase64Format: fileBase64Format,
            fileName: file.name
        }

        sign(requestParameters);
    }
    // reader.readAsArrayBuffer(file);
    reader.readAsDataURL(file);
});

function uncheckedAllPackagingFormats() {
    document.getElementById("packagingFormatInternallyDetached").checked = false;
    document.getElementById("packagingFormatEnveloped").checked = false;
    document.getElementById("packagingFormatDetached").checked = false;
    document.getElementById("packagingFormatEnveloping").checked = false;
}

function changeSignatureLevel(signatureFormat) {
    document.getElementById("signatureLevelBASELINE_B").checked = true;
    document.getElementById("signatureLevelBASELINE_B").value = signatureFormat + "-BASELINE-B";
    document.getElementById("signatureLevelBASELINE_B_label").innerText = signatureFormat + "-BASELINE_B";

    document.getElementById("signatureLevelBASELINE_T").value = signatureFormat + "-BASELINE-T";
    document.getElementById("signatureLevelBASELINE_T_label").innerText = signatureFormat + "-BASELINE_T";

    document.getElementById("signatureLevelBASELINE_LT").value = signatureFormat + "-BASELINE-LT";
    document.getElementById("signatureLevelBASELINE_LT_label").innerText = signatureFormat + "-BASELINE_LT";

    document.getElementById("signatureLevelBASELINE_LTA").value = signatureFormat + "-BASELINE-LTA";
    document.getElementById("signatureLevelBASELINE_LTA_label").innerText = signatureFormat + "-BASELINE_LTA";


    let selectedContainer = document.querySelector('input[name="container"]:checked').value;
    if ("no" === selectedContainer) {
        if ("CAdES" === signatureFormat) {
            document.getElementById("packagingFormatInternallyDetached").disabled = true;
            document.getElementById("packagingFormatEnveloped").disabled = true;
            document.getElementById("packagingFormatDetached").disabled = false;
            document.getElementById("packagingFormatEnveloping").disabled = false;

            uncheckedAllPackagingFormats();
        } else if ("PAdES" === signatureFormat) {
            document.getElementById("packagingFormatInternallyDetached").disabled = true;
            document.getElementById("packagingFormatEnveloping").disabled = true;
            document.getElementById("packagingFormatDetached").disabled = true;
            document.getElementById("packagingFormatEnveloped").disabled = false;

            document.getElementById("packagingFormatEnveloped").checked = true;
        } else if ("XAdES" === signatureFormat) {
            document.getElementById("packagingFormatInternallyDetached").disabled = false;
            document.getElementById("packagingFormatEnveloping").disabled = false;
            document.getElementById("packagingFormatDetached").disabled = false;
            document.getElementById("packagingFormatEnveloped").disabled = false;

            uncheckedAllPackagingFormats();
        }
    }
}

function changeContainer(container) {
    if ("no" === container) {
        document.getElementById("signatureFormatPAdES").disabled = false;

        document.getElementById("packagingFormatDetached").disabled = false;
        document.getElementById("packagingFormatEnveloping").disabled = false;

        document.getElementById("signatureFormatPAdES").checked = false;
        document.getElementById("signatureFormatCAdES").checked = false;
        document.getElementById("signatureFormatXAdES").checked = false;

        uncheckedAllPackagingFormats();
    } else if ("asic-s" === container) {
        document.getElementById("signatureFormatPAdES").disabled = true;

        document.getElementById("packagingFormatEnveloped").disabled = true;
        document.getElementById("packagingFormatInternallyDetached").disabled = true;
        document.getElementById("packagingFormatEnveloping").disabled = true;
        document.getElementById("packagingFormatDetached").disabled = false;

        document.getElementById("packagingFormatDetached").checked = true;
    } else if ("asic-e" === container) {
        document.getElementById("signatureFormatPAdES").disabled = true;

        document.getElementById("packagingFormatEnveloped").disabled = true;
        document.getElementById("packagingFormatInternallyDetached").disabled = true;
        document.getElementById("packagingFormatEnveloping").disabled = true;
        document.getElementById("packagingFormatDetached").disabled = false;

        document.getElementById("packagingFormatDetached").checked = true;
    }
}