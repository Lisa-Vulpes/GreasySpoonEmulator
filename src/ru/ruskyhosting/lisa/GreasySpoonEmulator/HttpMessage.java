/**----------------------------------------------------------------------------
 * GreasySpoon Emulator
 * Copyright 2013 Lisa
 * ----------------------------------------------------------------------------
 *
 * The MIT License
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files (the
 *  "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish,
 *  distribute, sublicense, and/or sell copies of the Software, and to
 *  permit persons to whom the Software is furnished to do so, subject to
 *  the following conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * ---------------------------------------------------------------------------*/
package ru.ruskyhosting.lisa.GreasySpoonEmulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>このプログラムは、GreasySpoonスクリプトを擬似的に試行できるようにします。</p>
 * <p>Javaサーブレットを使用して、Webページと通信を行います。<p>
 * <p>GreasySpoonのHttpMessageと同じような結果を返しますが、挙動が異なる場合もあるので注意してください。</p>
 * <p>使用方法は、以下の通りです。</p>
 * <code><pre>HttpMessage httpMessage = new HttpMessage(String url, MessageType messageType)</pre></code>
 * <p>以降はhttpMessageに対して操作を行ってください。</p>
 * <p><strong>現在はResponseにのみ対応しています。<strong></p>
 * @author Lisa
 */
public class HttpMessage {
    private String url;
    private URL _url;
    private HttpURLConnection connection;
    private ArrayList<String> responseHeaderNameList;
    private ArrayList<String> responseHeaderValueList;
    private int responseHeaderListLength;
    
    /**
     * HttpMessageのコンストラクタです。
     * 
     * @param url 対象URL
     */
    // TODO: MessageTypeの種類による挙動の変化
    public HttpMessage(String url, MessageType messageType) {
        this.url = url;
        try {
            _url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        connection = getConnection();
        this.setDefaultResponseHeaders();
    }
    
    /**
     * 対象URLを返します。
     * 
     * @return 対象URL
     */
    public String getUrl() {
        
        return url;
    }
    
    /**
     * HTTPレスポンスヘッダの内容を返します。
     * 
     * @return HTTPレスポンスヘッダの内容
     */
    public String getResponseHeaders() {      
        StringBuilder stringBuilder = new StringBuilder();
        
        /*
         * 以下のような書式でHTTPレスポンスヘッダを返す。
         *  Content-type: text/html
         *  Server: Apache
         *  ...
         */
        for (int i = 0; i < responseHeaderListLength; i++) {
            stringBuilder.append(responseHeaderNameList.get(i));
            stringBuilder.append(": ");
            stringBuilder.append(responseHeaderValueList.get(i));
            stringBuilder.append(System.getProperty("line.separator"));
        }
        String responseHeaders = stringBuilder.toString();
        
        return responseHeaders;
    }

    /**
     * 対象URLのHTMLを取得します。
     * 
     * @return 対象URLのHTML
     */
    public String getBody() {
        StringBuilder stringBuilder = new StringBuilder();
        
        try {
            InputStream inputStream = connection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            
            /*
             * 1行ずつ読み込み、毎行ごとに改行コードを挿入する
             */
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                } else {
                    stringBuilder.append(line);
                    stringBuilder.append(System.getProperty("line.separator"));
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String body = stringBuilder.toString();
       
        return body;
    }
    
    /**
     * 特定のHTTPレスポンスヘッダの値を返します。
     * 
     * @param key 対象となるHTTPレスポンスヘッダ
     * @return レスポンスヘッダの値、該当なしの場合はnull
     */
   
    public String getResponseHeader(String key) {
        String responseHeader = null;
        
        /*
         * 対象となるHTTPレスポンスヘッダが発見された場合、その種類のインデックス値に
         * 該当するレスポンスヘッダの値を返す。
         * 検索の初めにNullチェックを行わないと、NullPointerExceptionが発生する。
         */
        int i = 0;
        while (i < responseHeaderListLength) {
            if (responseHeaderNameList.get(i) == null) {
                i++;
            } else if (responseHeaderNameList.get(i).matches("(?i)" + key)) {
                responseHeader = responseHeaderValueList.get(i);
                break;
            } else {
                i++;
            }
        }
        
        if (responseHeader != null) {
            return responseHeader;
        } else {
            return null;
        }
    }
    
    /**
     * 特定のHTTPヘッダ情報を削除します。
     * 
     * @param headerName 削除するHTTPヘッダ名
     */
    public void deleteHeader(String headerName) {
        int i = 0;
        while (i < responseHeaderListLength) {
            if (responseHeaderNameList.get(i) == null) {
                i++;
            } else if (responseHeaderNameList.get(i).matches("(?i)" + headerName)) {
                responseHeaderNameList.remove(i);
                responseHeaderValueList.remove(i);
                responseHeaderListLength--;
                break;
            } else {
                i++;
            }
        }
    }
    
    /**
     * HTTPメッセージヘッダに指定されたヘッダ情報を追加します。
     * このメソッドではヘッダ情報の上書きは行われません。
     * 
     * @param headerName 追加するヘッダ名
     * @param headerValue 追加するヘッダ値
     */
    public void addHeader(String headerName, String headerValue) {
        responseHeaderNameList.add(headerName);
        responseHeaderValueList.add(headerValue);
        responseHeaderListLength++;
    }
    
    /**
     * レスポンスのステータスコードを返します。
     * 
     * @return レスポンスのステータスコード
     */
    public String getType() {
        /*
         * サーブレットで得られるステータスコードはint型ではあるが
         * GreasySpoonのステータスコードはString型なので、String型に変換して返す。
         */
        int rawStatusCode = 0;
        try {
            rawStatusCode = connection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String statusCode = String.valueOf(rawStatusCode);
        
        return statusCode;
    }
    
    /**
     * ヘッダ情報を書き換えます。
     * 
     * @param headerName 書き換えるヘッダ名
     * @param newValue 新しいヘッダ値
     */
    public void rewriteHeader(String headerName, String newValue) {
        int i = 0;
        while (i < responseHeaderListLength) {
            if (responseHeaderNameList.get(i) == null) {
                i++;
            } else if (responseHeaderNameList.get(i).matches("(?i)" + headerName)) {
                responseHeaderNameList.remove(i);
                responseHeaderValueList.remove(i);
                responseHeaderNameList.add(i, headerName);
                responseHeaderValueList.add(i, newValue);
                break;
            } else {
                i++;
            }
        }
    }
    
    /**
     * クライアントに送られるHTML(HTTP body)をセットします。
     * 内容は標準出力に表示されます。
     * 
     * @param newBody 表示したい新たなHTML(HTTP body)
     */
    public void setBody(String newBody) {
        System.out.println(newBody);
    }
    
    /**
     * 対象URLとの接続を確立します。
     *     * 
     * @return 対象URLとの接続
     */
    private HttpURLConnection getConnection() {
        // 参考: http://x68000.q-e-d.net/~68user/net/java-http-url-connection-1.html
        // 対象URLとの接続を開く
        try {
            connection = (HttpURLConnection) _url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // 対象URLにリクエストを送る
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
        
        // 対象URLのリダイレクトを無効にする
        connection.setInstanceFollowRedirects(false);
        
        // 対象URLに接続する
        try {
            connection.connect();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return connection;
    }
    
    /**
     * 対象URLのHTTPレスポンスヘッダ情報を返します。
     * 
     * @return レスポンスヘッダの種類と値
     */
    private void setDefaultResponseHeaders() {
        // ヘッダフィールドの取得
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        Iterator<String> iterator = headerFields.keySet().iterator();

        // ヘッダフィールドからヘッダ情報を集める
        String responseHeaderName = null;
        String responseHeaderValue = null;
        responseHeaderNameList = new ArrayList<String>();
        responseHeaderValueList = new ArrayList<String>();
        while (iterator.hasNext()) {
            responseHeaderName = iterator.next();
            responseHeaderValue = headerFields.get(responseHeaderName).toString().replaceAll("\\[", "").replaceAll("\\]","");
            responseHeaderNameList.add(responseHeaderName);
            responseHeaderValueList.add(responseHeaderValue);
        }
        responseHeaderListLength = responseHeaderNameList.size();
    }
}