function checkAgentStatus() {
    let request = new XMLHttpRequest();
    request.open("GET", "http://localhost:9795/nexu-info");
    request.send();
    request.onload = () => {
        if (request.status == 200) {
            let responseText = JSON.parse(request.response).version;
            console.log(JSON.parse(request.response).version);
            if ("1.23-SNAPSHOT" === responseText) {
                document.getElementById("message").style.color = "green";
                document.getElementById("message").innerText = "Успешно свързване с NexU локална компонента!";
                document.getElementById("submitButton").disabled = false;
            }
        } else {
            alert("ERROR status=" + request.status + " statusText=" + request.statusText);
        }
    }
}

function sign(requestParameters) {
    let request = new XMLHttpRequest();
    request.open("POST", "http://localhost:9795/rest/signDoc");
    request.send(JSON.stringify(requestParameters));
    request.onload = () => {
        if (request.status == 200) {
            alert(request.response);
        } else {
            alert("ERROR status=" + request.status + " statusText=" + request.statusText);
        }
    }
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

    alert(" Файл [" + file.name + "]" +
        "\n Размер на файла [" + file.size + " bytes]" +
        "\n Контейнер [" + container + "]" +
        "\n Формат на подписа [" + signatureFormat + "]" +
        "\n Тип на подписа [" + packagingFormat + "]" +
        "\n Ниво на подписa [" + signatureLevel + "]" +
        "\n Хеш алгоритъм [" + digestAlgorithm + "]"
    );

    let reader = new FileReader();
    reader.onloadend = function () {
        let arrayBuffer = this.result;
        var fileBase64Format = arrayBuffer.split(',')[1];
        // alert(arrayBuffer)
        // let fileByteArray = new Uint8Array(arrayBuffer);
        // let binaryString = String.fromCharCode.apply(null, fileByteArray);
        // alert("Файл byte array [" + fileByteArray + "]");

        let requestParameters = {
            container:container,
            signatureFormat:signatureFormat,
            packagingFormat:packagingFormat,
            signatureLevel:signatureLevel,
            digestAlgorithm:digestAlgorithm,
            fileBase64Format:fileBase64Format
        }

        sign(requestParameters);
    }
    // reader.readAsArrayBuffer(file);
    reader.readAsDataURL(file);
});

function changeSignatureLevel(signatureFormat) {
    document.getElementById("signatureLevelBASELINE_B").checked  = true;
    document.getElementById("signatureLevelBASELINE_B").value = signatureFormat + "_BASELINE_B";
    document.getElementById("signatureLevelBASELINE_B_label").innerText = signatureFormat + "-BASELINE_B";

    document.getElementById("signatureLevelBASELINE_T").value = signatureFormat + "BASELINE_T";
    document.getElementById("signatureLevelBASELINE_T_label").innerText = signatureFormat + "-BASELINE_T";

    document.getElementById("signatureLevelBASELINE_LT").value = signatureFormat + "BASELINE_LT";
    document.getElementById("signatureLevelBASELINE_LT_label").innerText = signatureFormat + "-BASELINE_LT";

    document.getElementById("signatureLevelBASELINE_LTA").value = signatureFormat + "BASELINE_LTA";
    document.getElementById("signatureLevelBASELINE_LTA_label").innerText = signatureFormat + "-BASELINE_LTA";
}

function changeContainer(container) {

}