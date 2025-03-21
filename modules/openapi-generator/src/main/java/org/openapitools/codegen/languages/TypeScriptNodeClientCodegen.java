/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.languages;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.Getter;
import lombok.Setter;
import org.openapitools.codegen.*;
import org.openapitools.codegen.meta.features.SecurityFeature;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.openapitools.codegen.utils.CamelizeOption.LOWERCASE_FIRST_LETTER;
import static org.openapitools.codegen.utils.StringUtils.camelize;

public class TypeScriptNodeClientCodegen extends AbstractTypeScriptClientCodegen {
    private final Logger LOGGER = LoggerFactory.getLogger(TypeScriptNodeClientCodegen.class);

    public static final String NPM_REPOSITORY = "npmRepository";
    private static final String DEFAULT_MODEL_FILENAME_DIRECTORY_PREFIX = "./";
    private static final String DEFAULT_MODEL_IMPORT_DIRECTORY_PREFIX = "../";

    @Getter @Setter
    protected String npmRepository = null;
    protected String apiSuffix = "Api";

    public TypeScriptNodeClientCodegen() {
        super();

        modifyFeatureSet(features -> features.includeSecurityFeatures(SecurityFeature.BearerToken));

        typeMapping.put("file", "RequestFile");
        // RequestFile is defined as: `type RequestFile = string | Buffer | ReadStream | RequestDetailedFile;`
        languageSpecificPrimitives.add("Buffer");
        languageSpecificPrimitives.add("ReadStream");
        languageSpecificPrimitives.add("RequestDetailedFile");
        languageSpecificPrimitives.add("RequestFile");

        // clear import mapping (from default generator) as TS does not use it
        // at the moment
        importMapping.clear();

        typeMapping.put("DateTime", "Date");

        outputFolder = "generated-code/typescript-node";
        embeddedTemplateDir = templateDir = "typescript-node";
        modelTemplateFiles.put("model.mustache", ".ts");
        apiTemplateFiles.put("api-single.mustache", ".ts");
        modelPackage = "model";
        apiPackage = "api";

        supportModelPropertyNaming(CodegenConstants.MODEL_PROPERTY_NAMING_TYPE.camelCase);
        this.cliOptions.add(new CliOption(NPM_REPOSITORY, "Use this property to set an url your private npmRepo in the package.json"));

    }

    @Override
    public String getName() {
        return "typescript-node";
    }

    @Override
    public String getHelp() {
        return "Generates a TypeScript NodeJS client library.";
    }

    @Override
    public boolean isDataTypeFile(final String dataType) {
        return "RequestFile".equals(dataType);
    }

    @Override
    public String getTypeDeclaration(Schema p) {
        if (ModelUtils.isFileSchema(p)) {
            // There are two file types:
            // 1) RequestFile: the parameter for the request lib when uploading a file
            // (https://github.com/request/request#multipartform-data-multipart-form-uploads)
            // 2) Buffer: for downloading files.
            // Use RequestFile as a default. The return type is fixed to Buffer in handleMethodResponse.
            return "RequestFile";
        } else if (ModelUtils.isBinarySchema(p)) {
            return "Buffer";
        }
        return super.getTypeDeclaration(p);
    }

    @Override
    protected void handleMethodResponse(Operation operation, Map<String, Schema> schemas, CodegenOperation op,
                                        ApiResponse methodResponse) {
        handleMethodResponse(operation, schemas, op, methodResponse, Collections.emptyMap());
    }

    @Override
    protected void handleMethodResponse(Operation operation,
                                        Map<String, Schema> schemas,
                                        CodegenOperation op,
                                        ApiResponse methodResponse,
                                        Map<String, String> importMappings) {
        super.handleMethodResponse(operation, schemas, op, methodResponse, importMappings);

        // see comment in getTypeDeclaration
        if (op.isResponseFile) {
            op.returnType = "Buffer";
        }
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "Default" + apiSuffix;
        }
        return camelize(name) + apiSuffix;
    }

    @Override
    public String toApiFilename(String name) {
        if (name.length() == 0) {
            return "default" + apiSuffix;
        }
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        }
        return camelize(name, LOWERCASE_FIRST_LETTER) + apiSuffix;
    }

    @Override
    public String toApiImport(String name) {
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        }

        return apiPackage() + "/" + toApiFilename(name);
    }

    @Override
    public String toModelFilename(String name) {
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        }

        return DEFAULT_MODEL_FILENAME_DIRECTORY_PREFIX + camelize(toModelName(name), LOWERCASE_FIRST_LETTER);
    }

    @Override
    public String toModelImport(String name) {
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        }

        return DEFAULT_MODEL_IMPORT_DIRECTORY_PREFIX + modelPackage() + "/" + camelize(toModelName(name), LOWERCASE_FIRST_LETTER);
    }

    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
        Map<String, ModelsMap> result = super.postProcessAllModels(objs);

        for (ModelsMap entry : result.values()) {
            for (ModelMap mo : entry.getModels()) {
                CodegenModel cm = mo.getModel();

                // Add additional filename information for imports
                mo.put("tsImports", toTsImports(cm, cm.imports));
            }
        }
        return result;
    }

    private List<Map<String, String>> toTsImports(CodegenModel cm, Set<String> imports) {
        List<Map<String, String>> tsImports = new ArrayList<>();
        for (String im : imports) {
            if (!im.equals(cm.classname)) {
                HashMap<String, String> tsImport = new HashMap<>();
                tsImport.put("classname", im);
                tsImport.put("filename", toModelFilename(removeModelPrefixSuffix(im)));
                tsImports.add(tsImport);
            }
        }
        return tsImports;
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap operations, List<ModelMap> allModels) {
        OperationMap objs = operations.getOperations();

        // The api.mustache template requires all of the auth methods for the whole api
        // Loop over all the operations and pick out each unique auth method
        Map<String, CodegenSecurity> authMethodsMap = new HashMap<>();
        for (CodegenOperation op : objs.getOperation()) {
            if (op.hasAuthMethods) {
                for (CodegenSecurity sec : op.authMethods) {
                    authMethodsMap.put(sec.name, sec);
                }
            }
        }

        // If there wer any auth methods specified add them to the operations context
        if (!authMethodsMap.isEmpty()) {
            operations.put("authMethods", authMethodsMap.values());
            operations.put("hasAuthMethods", true);
        }

        // Add filename information for api imports
        objs.put("apiFilename", getApiFilenameFromClassname(objs.getClassname()));

        // Add additional filename information for model imports in the apis
        List<Map<String, String>> imports = operations.getImports();
        for (Map<String, String> im : imports) {
            im.put("filename", im.get("import"));
        }

        return operations;
    }

    @Override
    public void processOpts() {
        super.processOpts();
        supportingFiles.add(new SupportingFile("models.mustache", modelPackage().replace('.', File.separatorChar), "models.ts"));
        supportingFiles.add(new SupportingFile("api-all.mustache", apiPackage().replace('.', File.separatorChar), "apis.ts"));
        supportingFiles.add(new SupportingFile("api.mustache", getIndexDirectory(), "api.ts"));
        supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
        supportingFiles.add(new SupportingFile("gitignore", "", ".gitignore"));

        if (additionalProperties.containsKey(NPM_NAME)) {
            addNpmPackageGeneration();
        }
    }

    private void addNpmPackageGeneration() {

        if (additionalProperties.containsKey(NPM_REPOSITORY)) {
            this.setNpmRepository(additionalProperties.get(NPM_REPOSITORY).toString());
        }

        //Files for building our lib
        supportingFiles.add(new SupportingFile("package.mustache", getPackageRootDirectory(), "package.json"));
        supportingFiles.add(new SupportingFile("tsconfig.mustache", getPackageRootDirectory(), "tsconfig.json"));
    }

    private String getIndexDirectory() {
        String indexPackage = modelPackage.substring(0, Math.max(0, modelPackage.lastIndexOf('.')));
        return indexPackage.replace('.', File.separatorChar);
    }

    // The purpose of this override and associated methods is to allow for automatic conversion
    // from 'file' type to the built in node 'Buffer' type
    @Override
    public String getSchemaType(Schema p) {
        String openAPIType = super.getSchemaType(p);
        if (isLanguagePrimitive(openAPIType) || isLanguageGenericType(openAPIType)) {
            return openAPIType;
        }
        return applyLocalTypeMapping(openAPIType);
    }

    private String applyLocalTypeMapping(String type) {
        if (typeMapping.containsKey(type)) {
            return typeMapping.get(type);
        }
        return type;
    }

    private boolean isLanguagePrimitive(String type) {
        return languageSpecificPrimitives.contains(type);
    }

    // Determines if the given type is a generic/templated type (ie. ArrayList<String>)
    private boolean isLanguageGenericType(String type) {
        for (String genericType : languageGenericTypes) {
            if (type.startsWith(genericType + "<")) {
                return true;
            }
        }
        return false;
    }

    private String getPackageRootDirectory() {
        String indexPackage = modelPackage.substring(0, Math.max(0, modelPackage.lastIndexOf('.')));
        return indexPackage.replace('.', File.separatorChar);
    }

    private String getApiFilenameFromClassname(String classname) {
        String name = classname.substring(0, classname.length() - apiSuffix.length());
        return toApiFilename(name);
    }

    private String removeModelPrefixSuffix(String name) {
        String result = name;
        final String prefix = capitalize(this.modelNamePrefix);
        final String suffix = capitalize(this.modelNameSuffix);

        if (prefix.length() > 0 && result.startsWith(prefix)) {
            result = result.substring(prefix.length());
        }
        if (suffix.length() > 0 && result.endsWith(suffix)) {
            result = result.substring(0, result.length() - suffix.length());
        }
        return result;
    }

    @Override
    protected void addAdditionPropertiesToCodeGenModel(CodegenModel codegenModel, Schema schema) {
        super.addAdditionPropertiesToCodeGenModel(codegenModel, schema);
        Schema additionalProperties = ModelUtils.getAdditionalProperties(schema);
        codegenModel.additionalPropertiesType = getSchemaType(additionalProperties);
        if ("array".equalsIgnoreCase(codegenModel.additionalPropertiesType)) {
            codegenModel.additionalPropertiesType += '<' + getSchemaType((ModelUtils.getSchemaItems(additionalProperties))) + '>';
        }
        addImport(codegenModel, codegenModel.additionalPropertiesType);
    }

    @Override
    public String toDefaultValue(Schema p) {
        String def = super.toDefaultValue(p);
        if ("undefined".equals(def)) {
            return null;
        }
        return def;
    }

    @Override
    public String toEnumDefaultValue(String value, String datatype) {
        return datatype + "." + value;
    }
}
