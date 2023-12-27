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

;(function () {
    'use strict';

    if (typeof Wicket === 'undefined') {
        window.Wicket = {};
    }

    window.Wicket.WUPB = Wicket.Class.create();
    window.Wicket.WUPB.prototype = {
        initialize: function (formid, statusid, barid, url, fileid, initialStatus, onProgressUpdated) {
            this.statusid = statusid;
            this.barid = barid;
            this.url = url;
            this.fileid = fileid;
            this.initialStatus = initialStatus;
            this.onProgressUpdated = onProgressUpdated;

            if (formid) {
                this.setupFormSubmitListener(formid);
            }
        },

        setupFormSubmitListener: function (formid) {
            var formElement = Wicket.$(formid);
            this.originalCallback = formElement.onsubmit;
            formElement.onsubmit = Wicket.bind(this.submitCallback, this);
        },

        submitCallback: function () {
            if (this.originalCallback && !this.originalCallback()) {
                return false;
            }
            this.start();
            return true;
        },

        start: function () {
            var displayprogress = this.checkDisplayProgress();
            if (displayprogress) {
                this.initializeProgress();
                this.showStatusAndBar();
                this.scheduleUpdate();
            }
        },

        checkDisplayProgress: function () {
            if (this.fileid) {
                var fileupload = Wicket.$(this.fileid);
                return fileupload && fileupload.value;
            }
            return true;
        },

        initializeProgress: function () {
            this.setPercent(0);
            this.setStatus(this.initialStatus);
        },

        showStatusAndBar: function () {
            Wicket.DOM.show(Wicket.$(this.statusid));
            Wicket.DOM.show(Wicket.$(this.barid));
        },

        setStatus: function (status) {
            var label = document.createElement('label');
            label.innerHTML = status;
            var $statusId = Wicket.$(this.statusid);
            if ($statusId) {
                var oldLabel = $statusId.firstChild;
                if (oldLabel) {
                    $statusId.removeChild(oldLabel);
                }
                $statusId.appendChild(label);
            }
        },

        setPercent: function (progressPercent) {
            var barId = Wicket.$(this.barid);
            if (barId && barId.firstChild && barId.firstChild.firstChild) {
                barId.firstChild.firstChild.style.width = progressPercent + '%';
            }
            if (this.onProgressUpdated) {
                this.onProgressUpdated(progressPercent);
            }
        },

        scheduleUpdate: function () {
            window.setTimeout(Wicket.bind(this.load, this), 1000);
        },

        _createIFrame: function (iframeName) {
            var $iframe = jQuery('<iframe name="' + iframeName + '" id="' + iframeName +
                '" src="about:blank" hidden=""></iframe>');
            return $iframe[0];
        },

        load: function () {
            var URL = this.url;
            this.iframe = this._createIFrame("" + Math.random());
            document.body.appendChild(this.iframe);
            Wicket.Event.add(this.iframe, "load", Wicket.bind(this.update, this));
            this.iframe.src = URL;
        },

        update: function () {
            var responseAsText = this.getResponseText();
            var update = responseAsText.split('|');
            var progressPercent = update[1];
            var status = update[2];
            this.updateProgressAndStatus(progressPercent, status);
            this.handleCompletion(progressPercent);
        },

        getResponseText: function () {
            if (this.iframe.contentDocument) {
                return this.iframe.contentDocument.body.innerHTML;
            }
            return this.iframe.contentWindow.document.body.innerHTML;
        },

        updateProgressAndStatus: function (progressPercent, status) {
            this.setPercent(progressPercent);
            this.setStatus(status);
            this.cleanupIframe();
        },

        cleanupIframe: function () {
            this.iframe.parentNode.removeChild(this.iframe);
            this.iframe = null;
        },

        handleCompletion: function (progressPercent) {
            if (progressPercent === '100') {
                this.hideStatusAndBar();
            } else {
                this.scheduleUpdate();
            }
        },

        hideStatusAndBar: function () {
            Wicket.DOM.hide(Wicket.$(this.statusid));
            Wicket.DOM.hide(Wicket.$(this.barid));
        }
    };
})();
