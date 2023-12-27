/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

;(function() {
    'use strict';

    if (typeof Wicket.FileUploadToResourceField === 'object') {
        return;
    }

    function buildResourceUrl(settings) {
        var resourceUrl = settings.resourceUrl + "?uploadId=" + settings.inputName + "&maxSize=" + settings.maxSize;
        if (settings.fileMaxSize != null) {
            resourceUrl += "&fileMaxSize=" + settings.fileMaxSize;
        }
        if (settings.fileCountMax != null) {
            resourceUrl += "&fileCountMax=" + settings.fileCountMax;
        }
        return resourceUrl;
    }

    function createFormData(input) {
        var formData = new FormData();
        for (var index = 0; index < input.files.length; index++) {
            formData.append("WICKET-FILE-UPLOAD", input.files[index]);
        }
        return formData;
    }

    function handleSuccess(res, self) {
        if (res.error) {
            self.uploadErrorCallBack(res);
        } else {
            self.clientSideSuccessCallBack();
            var ep = {'error': false, 'filesInfo': JSON.stringify(res)};
            Wicket.Ajax.get({"u": self.ajaxCallBackUrl, "ep": ep});
        }
    }

    function handleError(jqXHR, textStatus, errorThrown, self) {
        var ep;
        if (textStatus === "abort") {
            ep = {'error': true, 'errorMessage': 'upload.canceled'};
            Wicket.Ajax.get({"u": self.ajaxCallBackUrl, "ep": ep});
        } else if (textStatus === "error"){
            ep = {'error': true, "errorMessage": errorThrown};
            self.uploadErrorCallBack(ep);
            Wicket.Ajax.get({"u": self.ajaxCallBackUrl, "ep": ep});
        } else if (textStatus === "parsererror"){
            var data = jqXHR.responseText;
            Wicket.Log.log(data);
        }
    }

    Wicket.FileUploadToResourceField = function (settings, clientBeforeSendCallBack, clientSideSuccessCallBack, clientSideCancelCallBack, uploadErrorCallBack) {
        this.settings = settings;
        this.inputName = settings.inputName;
        this.input = document.getElementById(this.inputName);
        this.resourceUrl = buildResourceUrl(this.settings);
        this.ajaxCallBackUrl = settings.ajaxCallBackUrl;
        this.clientBeforeSendCallBack = clientBeforeSendCallBack;
        this.clientSideSuccessCallBack = clientSideSuccessCallBack;
        this.clientSideCancelCallBack = clientSideCancelCallBack;
        this.uploadErrorCallBack = uploadErrorCallBack;
    };

    Wicket.FileUploadToResourceField.prototype.upload = function() {
        this.input = document.getElementById(this.inputName);
        var formData = createFormData(this.input);
        var self = this;

        this.xhr = $.ajax({
            url: this.resourceUrl,
            type: "POST",
            data: formData,
            processData: false,
            contentType: false,
            success: function (res) {
                handleSuccess(res, self);
            },
            beforeSend: function (xhr) {
                self.clientBeforeSendCallBack(xhr);
            },
            error: function (jqXHR, textStatus, errorThrown) {
                handleError(jqXHR, textStatus, errorThrown, self);
            }
        });
    };

    Wicket.FileUploadToResourceField.prototype.cancel = function () {
        if (this.xhr) {
            this.xhr.abort();
            this.clientSideCancelCallBack();
            Wicket.Log.log("The upload associated with field '" + this.inputName + "' has been canceled!");
            delete this.xhr;
        } else {
            Wicket.Log.log("Too late to cancel upload for field '"  + this.inputName +  "': the upload has already finished.");
        }
    };
})();