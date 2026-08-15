package com.ao3.app.translation

import android.webkit.WebView

/**
 * 注入到页面里的翻译驱动脚本。
 * 采用懒翻译：只翻译当前可见区域（含上下各一个半屏的缓冲），
 * 滚动到新内容时再由 IntersectionObserver 触发下一批翻译。
 */
object TranslationScript {

    const val BRIDGE_NAME = "ao3TranslationBridge"

    val INSTALL_SCRIPT: String = """
        (function () {
          if (window.__ao3Translation) return;
          var SKIP = {SCRIPT:1,STYLE:1,NOSCRIPT:1,TEXTAREA:1,INPUT:1,SELECT:1,OPTION:1,CODE:1,PRE:1,IFRAME:1};
          var BATCH = 30;
          var MIN_LEN = 3;
          var entries = [];
          var parentMap = null;
          var queue = [];
          var translating = false;
          var active = false;
          var observer = null;

          function bridge() {
            return window.ao3TranslationBridge;
          }

          function collect() {
            entries = [];
            parentMap = new Map();
            var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
              acceptNode: function (node) {
                var parent = node.parentNode;
                if (!parent || parent.nodeType !== 1 || SKIP[parent.tagName] || parent.isContentEditable) {
                  return NodeFilter.FILTER_REJECT;
                }
                var value = node.nodeValue;
                if (!value || value.trim().length < MIN_LEN) {
                  return NodeFilter.FILTER_REJECT;
                }
                return NodeFilter.FILTER_ACCEPT;
              }
            });
            while (walker.nextNode()) {
              var node = walker.currentNode;
              var parent = node.parentNode;
              var index = entries.length;
              entries.push({
                node: node,
                original: node.nodeValue,
                translated: false,
                queued: false
              });
              if (!parentMap.has(parent)) parentMap.set(parent, []);
              parentMap.get(parent).push(index);
            }
          }

          function enqueue(index) {
            var entry = entries[index];
            if (entry && !entry.translated && !entry.queued) {
              entry.queued = true;
              queue.push(index);
            }
          }

          function processQueue() {
            if (translating) return;
            if (queue.length === 0) {
              var total = entries.length;
              var translatedCount = 0;
              for (var i = 0; i < total; i++) {
                if (entries[i].translated) translatedCount++;
              }
              var b = bridge();
              if (b && (translatedCount > 0 || total === 0)) {
                b.onIdle(translatedCount, total);
              }
              return;
            }
            var batch = [];
            var take = Math.min(BATCH, queue.length);
            for (var j = 0; j < take; j++) {
              var idx = queue.shift();
              batch.push({i: idx, t: entries[idx].original});
            }
            var b = bridge();
            if (!b) {
              translating = false;
              return;
            }
            translating = true;
            b.onBatchStarted();
            b.translateBatch(JSON.stringify(batch));
          }

          function startObserver() {
            if (!parentMap || parentMap.size === 0) return;
            if (observer) observer.disconnect();
            observer = new IntersectionObserver(function (records) {
              var dirty = false;
              for (var k = 0; k < records.length; k++) {
                if (records[k].isIntersecting) {
                  var list = parentMap.get(records[k].target);
                  if (list) {
                    for (var m = 0; m < list.length; m++) enqueue(list[m]);
                    dirty = true;
                  }
                }
              }
              if (dirty) processQueue();
            }, {rootMargin: '150% 0px 150% 0px'});
            parentMap.forEach(function (list, element) {
              observer.observe(element);
            });
          }

          function restoreAll() {
            for (var i = 0; i < entries.length; i++) {
              var entry = entries[i];
              if (entry.translated && entry.node && entry.original !== null) {
                entry.node.nodeValue = entry.original;
              }
              entry.translated = false;
              entry.queued = false;
            }
            queue = [];
            translating = false;
          }

          window.__ao3Translation = {
            translate: function () {
              if (translating) return false;
              restoreAll();
              collect();
              active = true;
              if (entries.length === 0) {
                var b = bridge();
                if (b) b.onIdle(0, 0);
                return true;
              }
              startObserver();
              processQueue();
              return true;
            },
            restore: function () {
              restoreAll();
              active = false;
              if (observer) observer.disconnect();
              observer = null;
              return true;
            },
            applyResults: function (results) {
              if (!active) {
                translating = false;
                return;
              }
              for (var i = 0; i < results.length; i++) {
                var item = results[i];
                var entry = entries[item.i];
                if (entry && entry.node && item.t !== null) {
                  entry.node.nodeValue = item.t;
                  entry.translated = true;
                  entry.queued = false;
                }
              }
              translating = false;
              processQueue();
            },
            isTranslating: function () {
              return translating;
            }
          };
        })();
    """.trimIndent()

    fun install(webView: WebView?) {
        webView?.evaluateJavascript(INSTALL_SCRIPT, null)
    }

    fun start(webView: WebView, onResult: ((Boolean) -> Unit)? = null) {
        webView.evaluateJavascript(
            "window.__ao3Translation ? window.__ao3Translation.translate() : false",
            { result -> onResult?.invoke(result == "true") },
        )
    }

    fun restore(webView: WebView) {
        webView.evaluateJavascript(
            "window.__ao3Translation && window.__ao3Translation.restore()",
            null,
        )
    }
}
