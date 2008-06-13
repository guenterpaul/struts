/*
 * $Id$
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.struts2.interceptor;

import java.io.File;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.multipart.MultiPartRequestWrapper;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.ValidationAware;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.opensymphony.xwork2.util.LocalizedTextUtil;
import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;

/**
 * <!-- START SNIPPET: description -->
 * <p/>
 * Interceptor that is based off of {@link MultiPartRequestWrapper}, which is automatically applied for any request that
 * includes a file. It adds the following parameters, where [File Name] is the name given to the file uploaded by the
 * HTML form:
 * <p/>
 * <ul>
 * <p/>
 * <li>[File Name] : File - the actual File</li>
 * <p/>
 * <li>[File Name]ContentType : String - the content type of the file</li>
 * <p/>
 * <li>[File Name]FileName : String - the actual name of the file uploaded (not the HTML name)</li>
 * <p/>
 * </ul>
 * <p/>
 * <p/> You can get access to these files by merely providing setters in your action that correspond to any of the three
 * patterns above, such as setDocument(File document), setDocumentContentType(String contentType), etc.
 * <br/>See the example code section.
 * <p/>
 * <p/> This interceptor will add several field errors, assuming that the action implements {@link ValidationAware}.
 * These error messages are based on several i18n values stored in struts-messages.properties, a default i18n file
 * processed for all i18n requests. You can override the text of these messages by providing text for the following
 * keys:
 * <p/>
 * <ul>
 * <p/>
 * <li>struts.messages.error.uploading - a general error that occurs when the file could not be uploaded</li>
 * <p/>
 * <li>struts.messages.error.file.too.large - occurs when the uploaded file is too large</li>
 * <p/>
 * <li>struts.messages.error.content.type.not.allowed - occurs when the uploaded file does not match the expected
 * content types specified</li>
 * <p/>
 * </ul>
 * <p/>
 * <!-- END SNIPPET: description -->
 * <p/>
 * <p/> <u>Interceptor parameters:</u>
 * <p/>
 * <!-- START SNIPPET: parameters -->
 * <p/>
 * <ul>
 * <p/>
 * <li>maximumSize (optional) - the maximum size (in bytes) that the interceptor will allow a file reference to be set
 * on the action. Note, this is <b>not</b> related to the various properties found in struts.properties.
 * Default to approximately 2MB.</li>
 * <p/>
 * <li>allowedTypes (optional) - a comma separated list of content types (ie: text/html) that the interceptor will allow
 * a file reference to be set on the action. If none is specified allow all types to be uploaded.</li>
 * <p/>
 * </ul>
 * <p/>
 * <!-- END SNIPPET: parameters -->
 * <p/>
 * <p/> <u>Extending the interceptor:</u>
 * <p/>
 * <p/>
 * <p/>
 * <!-- START SNIPPET: extending -->
 * <p/>
 * You can extend this interceptor and override the acceptFile method to provide more control over which files
 * are supported and which are not.
 * <p/>
 * <!-- END SNIPPET: extending -->
 * <p/>
 * <p/> <u>Example code:</u>
 * <p/>
 * <pre>
 * <!-- START SNIPPET: example-configuration -->
 * &lt;action name="doUpload" class="com.example.UploadAction"&gt;
 *     &lt;interceptor-ref name="fileUpload"/&gt;
 *     &lt;interceptor-ref name="basicStack"/&gt;
 *     &lt;result name="success"&gt;good_result.jsp&lt;/result&gt;
 * &lt;/action&gt;
 * <!-- END SNIPPET: example-configuration -->
 * </pre>
 * <p/>
 * <!-- START SNIPPET: multipart-note -->
 * <p/>
 * You must set the encoding to <code>multipart/form-data</code> in the form where the user selects the file to upload.
 * <p/>
 * <!-- END SNIPPET: multipart-note -->
 * <p/>
 * <pre>
 * <!-- START SNIPPET: example-form -->
 *   &lt;s:form action="doUpload" method="post" enctype="multipart/form-data"&gt;
 *       &lt;s:file name="upload" label="File"/&gt;
 *       &lt;s:submit/&gt;
 *   &lt;/s:form&gt;
 * <!-- END SNIPPET: example-form -->
 * </pre>
 * <p/>
 * And then in your action code you'll have access to the File object if you provide setters according to the
 * naming convention documented in the start.
 * <p/>
 * <pre>
 * <!-- START SNIPPET: example-action -->
 *    package com.example;
 * <p/>
 *    import java.io.File;
 *    import com.opensymphony.xwork2.ActionSupport;
 * <p/>
 *    public UploadAction extends ActionSupport {
 *       private File file;
 *       private String contentType;
 *       private String filename;
 * <p/>
 *       public void setUpload(File file) {
 *          this.file = file;
 *       }
 * <p/>
 *       public void setUploadContentType(String contentType) {
 *          this.contentType = contentType;
 *       }
 * <p/>
 *       public void setUploadFileName(String filename) {
 *          this.filename = filename;
 *       }
 * <p/>
 *       public String execute() {
 *          //...
 *          return SUCCESS;
 *       }
 *  }
 * <!-- END SNIPPET: example-action -->
 * </pre>
 */
public class FileUploadInterceptor extends AbstractInterceptor {

    private static final long serialVersionUID = -4764627478894962478L;

    protected static final Logger LOG = LoggerFactory.getLogger(FileUploadInterceptor.class);
    private static final String DEFAULT_DELIMITER = ",";
    private static final String DEFAULT_MESSAGE = "no.message.found";

    protected Long maximumSize;
    protected String allowedTypes;
    protected Set allowedTypesSet = Collections.EMPTY_SET;

    /**
     * Sets the allowed mimetypes
     *
     * @param allowedTypes A comma-delimited list of types
     */
    public void setAllowedTypes(String allowedTypes) {
        this.allowedTypes = allowedTypes;

        // set the allowedTypes as a collection for easier access later
        allowedTypesSet = getDelimitedValues(allowedTypes);
    }

    /**
     * Sets the maximum size of an uploaded file
     *
     * @param maximumSize The maximum size in bytes
     */
    public void setMaximumSize(Long maximumSize) {
        this.maximumSize = maximumSize;
    }

    /* (non-Javadoc)
     * @see com.opensymphony.xwork2.interceptor.Interceptor#intercept(com.opensymphony.xwork2.ActionInvocation)
     */
    public String intercept(ActionInvocation invocation) throws Exception {
        ActionContext ac = invocation.getInvocationContext();
        HttpServletRequest request = (HttpServletRequest) ac.get(ServletActionContext.HTTP_REQUEST);

        if (!(request instanceof MultiPartRequestWrapper)) {
            if (LOG.isDebugEnabled()) {
                ActionProxy proxy = invocation.getProxy();
                LOG.debug(getTextMessage("struts.messages.bypass.request", new Object[]{proxy.getNamespace(), proxy.getActionName()}, ActionContext.getContext().getLocale()));
            }

            return invocation.invoke();
        }

        final Object action = invocation.getAction();
        ValidationAware validation = null;

        if (action instanceof ValidationAware) {
            validation = (ValidationAware) action;
        }

        MultiPartRequestWrapper multiWrapper = (MultiPartRequestWrapper) request;

        if (multiWrapper.hasErrors()) {
            for (Iterator errorIter = multiWrapper.getErrors().iterator(); errorIter.hasNext();) {
                String error = (String) errorIter.next();

                if (validation != null) {
                    validation.addActionError(error);
                }

                LOG.error(error);
            }
        }

        Map parameters = ac.getParameters();

        // Bind allowed Files
        Enumeration fileParameterNames = multiWrapper.getFileParameterNames();
        while (fileParameterNames != null && fileParameterNames.hasMoreElements()) {
            // get the value of this input tag
            String inputName = (String) fileParameterNames.nextElement();

            // get the content type
            String[] contentType = multiWrapper.getContentTypes(inputName);

            if (isNonEmpty(contentType)) {
                // get the name of the file from the input tag
                String[] fileName = multiWrapper.getFileNames(inputName);

                if (isNonEmpty(fileName)) {
                    // Get a File object for the uploaded File
                    File[] files = multiWrapper.getFiles(inputName);
                    if (files != null && files.length > 0) {
                        ArrayList<File> acceptedFiles = new ArrayList<File>(files.length);
                        ArrayList<String> acceptedContentTypes = new ArrayList<String>(files.length);
                        ArrayList<String> acceptedFileNames = new ArrayList<String>(files.length);
                        String contentTypeName = inputName + "ContentType";
                        String fileNameName = inputName + "FileName";
                        for (int index = 0; index < files.length; index++) {
                            if (acceptFile(files[index], contentType[index], inputName, validation, ac.getLocale())) {
                                acceptedFiles.add(files[index]);
                                acceptedContentTypes.add(contentType[index]);
                                acceptedFileNames.add(fileName[index]);
                            }
                        }
                        if (acceptedFiles.size() != 0) {
                            parameters.put(inputName, acceptedFiles.toArray(new File[acceptedFiles.size()]));
                            parameters.put(contentTypeName, acceptedContentTypes.toArray(new String[acceptedContentTypes.size()]));
                            parameters.put(fileNameName, acceptedFileNames.toArray(new String[acceptedFileNames.size()]));
                        }
                    }
                } else {
                    LOG.error(getTextMessage("struts.messages.invalid.file", new Object[]{inputName}, ActionContext.getContext().getLocale()));
                }
            } else {
                LOG.error(getTextMessage("struts.messages.invalid.content.type", new Object[]{inputName}, ActionContext.getContext().getLocale()));
            }
        }

        // invoke action
        String result = invocation.invoke();

        // cleanup
        fileParameterNames = multiWrapper.getFileParameterNames();
        while (fileParameterNames != null && fileParameterNames.hasMoreElements()) {
            String inputValue = (String) fileParameterNames.nextElement();
            File[] file = multiWrapper.getFiles(inputValue);
            for (int index = 0; index < file.length; index++) {
                File currentFile = file[index];
                if (LOG.isInfoEnabled()) {
                    LOG.info(getTextMessage("struts.messages.removing.file", new Object[]{inputValue, currentFile}, ActionContext.getContext().getLocale()));
                }
                if ((currentFile != null) && currentFile.isFile()) {
                    currentFile.delete();
                }
            }
        }

        return result;
    }

    /**
     * Override for added functionality. Checks if the proposed file is acceptable based on contentType and size.
     *
     * @param file        - proposed upload file.
     * @param contentType - contentType of the file.
     * @param inputName   - inputName of the file.
     * @param validation  - Non-null ValidationAware if the action implements ValidationAware, allowing for better
     *                    logging.
     * @param locale
     * @return true if the proposed file is acceptable by contentType and size.
     */
    protected boolean acceptFile(File file, String contentType, String inputName, ValidationAware validation, Locale locale) {
        boolean fileIsAcceptable = false;

        // If it's null the upload failed
        if (file == null) {
            String errMsg = getTextMessage("struts.messages.error.uploading", new Object[]{inputName}, locale);
            if (validation != null) {
                validation.addFieldError(inputName, errMsg);
            }

            LOG.error(errMsg);
        } else if (maximumSize != null && maximumSize.longValue() < file.length()) {
            String errMsg = getTextMessage("struts.messages.error.file.too.large", new Object[]{inputName, file.getName(), "" + file.length()}, locale);
            if (validation != null) {
                validation.addFieldError(inputName, errMsg);
            }

            LOG.error(errMsg);
        } else if ((!allowedTypesSet.isEmpty()) && (!containsItem(allowedTypesSet, contentType))) {
            String errMsg = getTextMessage("struts.messages.error.content.type.not.allowed", new Object[]{inputName, file.getName(), contentType}, locale);
            if (validation != null) {
                validation.addFieldError(inputName, errMsg);
            }

            LOG.error(errMsg);
        } else {
            fileIsAcceptable = true;
        }

        return fileIsAcceptable;
    }

    /**
     * @param itemCollection - Collection of string items (all lowercase).
     * @param key            - Key to search for.
     * @return true if itemCollection contains the key, false otherwise.
     */
    private static boolean containsItem(Collection itemCollection, String key) {
        return itemCollection.contains(key.toLowerCase());
    }

    private static Set getDelimitedValues(String delimitedString) {
        Set<String> delimitedValues = new HashSet<String>();
        if (delimitedString != null) {
            StringTokenizer stringTokenizer = new StringTokenizer(delimitedString, DEFAULT_DELIMITER);
            while (stringTokenizer.hasMoreTokens()) {
                String nextToken = stringTokenizer.nextToken().toLowerCase().trim();
                if (nextToken.length() > 0) {
                    delimitedValues.add(nextToken);
                }
            }
        }
        return delimitedValues;
    }

    private static boolean isNonEmpty(Object[] objArray) {
        boolean result = false;
        for (int index = 0; index < objArray.length && !result; index++) {
            if (objArray[index] != null) {
                result = true;
            }
        }
        return result;
    }

    private String getTextMessage(String messageKey, Object[] args, Locale locale) {
        if (args == null || args.length == 0) {
            return LocalizedTextUtil.findText(this.getClass(), messageKey, locale);
        } else {
            return LocalizedTextUtil.findText(this.getClass(), messageKey, locale, DEFAULT_MESSAGE, args);
        }
    }
}
