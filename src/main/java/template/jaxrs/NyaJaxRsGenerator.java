package template.jaxrs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.AbstractJavaJAXRSServerCodegen;

import io.swagger.v3.oas.models.Operation;

public class NyaJaxRsGenerator extends AbstractJavaJAXRSServerCodegen {

	public NyaJaxRsGenerator() {
		super();

		artifactId = "openapi-jaxrs-resteasy-server";
		outputFolder = "generated-code/JavaJaxRS-Resteasy";

		// clioOptions default redifinition need to be updated
		updateOption(CodegenConstants.ARTIFACT_ID, this.getArtifactId());

		apiTemplateFiles.put("apiService.mustache", ".java");
		apiTemplateFiles.put("apiServiceImpl.mustache", ".java");
		apiTestTemplateFiles.clear(); // TODO: add test template

		// clear model and api doc template as AbstractJavaJAXRSServerCodegen
		// does not support auto-generated markdown doc at the moment
		//TODO: add doc templates
		modelDocTemplateFiles.remove("model_doc.mustache");
		apiDocTemplateFiles.remove("api_doc.mustache");

		embeddedTemplateDir = templateDir = "JavaJaxRS" + File.separator + "nya";

	}

	@Override
	public void processOpts() {
		super.processOpts();

		supportingFiles.add(
				new SupportingFile("ApiException.mustache", (sourceFolder + '/' + apiPackage).replace(".", "/"), "ApiException.java"));
		supportingFiles.add(new SupportingFile("ApiOriginFilter.mustache", (sourceFolder + '/' + apiPackage).replace(".", "/"),
				"ApiOriginFilter.java"));
		supportingFiles.add(new SupportingFile("ApiResponseMessage.mustache", (sourceFolder + '/' + apiPackage).replace(".", "/"),
				"ApiResponseMessage.java"));
		supportingFiles.add(new SupportingFile("NotFoundException.mustache", (sourceFolder + '/' + apiPackage).replace(".", "/"),
				"NotFoundException.java"));

		writeOptional(outputFolder, new SupportingFile("RestApplication.mustache",
				(sourceFolder + '/' + invokerPackage).replace(".", "/"), "RestApplication.java"));

		supportingFiles.add(new SupportingFile("OffsetDateTimeProvider.mustache", (sourceFolder + '/' + apiPackage).replace(".", "/"),
				"OffsetDateTimeProvider.java"));
		supportingFiles.add(new SupportingFile("LocalDateProvider.mustache", (sourceFolder + '/' + apiPackage).replace(".", "/"),
				"LocalDateProvider.java"));

	}

	@Override
	public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co,
			Map<String, List<CodegenOperation>> operations) {
		String basePath = resourcePath;
		if (basePath.startsWith("/")) {
			basePath = basePath.substring(1);
		}
		int pos = basePath.indexOf("/");
		if (pos > 0) {
			basePath = basePath.substring(0, pos);
		}

		if (StringUtils.isEmpty(basePath)) {
			basePath = "default";
		} else {
			if (co.path.startsWith("/" + basePath)) {
				co.path = co.path.substring(("/" + basePath).length());
			}
			co.subresourceOperation = !co.path.isEmpty();
		}
		List<CodegenOperation> opList = operations.get(basePath);
		if (opList == null || opList.isEmpty()) {
			opList = new ArrayList<CodegenOperation>();
			operations.put(basePath, opList);
		}
		opList.add(co);
		co.baseName = basePath;
	}

	@Override
	public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> objs, List<Object> allModels) {
		return super.postProcessOperationsWithModels(objs, allModels);
	}

	@Override
	public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
		super.postProcessModelProperty(model, property);

		//Add imports for Jackson
		if (!BooleanUtils.toBoolean(model.isEnum)) {
			model.imports.add("JsonProperty");

			if (BooleanUtils.toBoolean(model.hasEnums)) {
				model.imports.add("JsonValue");
			}
		}

		model.imports.remove("ApiModelProperty");
		model.imports.remove("ApiModel");
	}

	@Override
	public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
		objs = super.postProcessModelsEnum(objs);

		//Add imports for Jackson
		List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
		List<Object> models = (List<Object>) objs.get("models");
		for (Object _mo : models) {
			Map<String, Object> mo = (Map<String, Object>) _mo;
			CodegenModel cm = (CodegenModel) mo.get("model");
			// for enum model
			if (Boolean.TRUE.equals(cm.isEnum) && cm.allowableValues != null) {
				cm.imports.add(importMapping.get("JsonValue"));
				Map<String, String> item = new HashMap<String, String>();
				item.put("import", importMapping.get("JsonValue"));
				imports.add(item);

				cm.imports.remove("ApiModel");

				imports.removeIf(i -> i.containsValue("io.swagger.annotations.ApiModel"));

			}
		}

		return objs;
	}

}
