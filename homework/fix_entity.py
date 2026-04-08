import re

path = "src/main/java/com/jlm/homework/entity/WrongTitleBook.java"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

# Add Jackson imports if missing
if "import com.fasterxml.jackson.annotation.JsonIgnore;" not in code:
    code = code.replace("import lombok.Data;", "import lombok.Data;\nimport com.fasterxml.jackson.annotation.JsonIgnore;\nimport com.fasterxml.jackson.annotation.JsonProperty;\nimport com.alibaba.cloud.commons.lang.StringUtils;")

# Apply JsonIgnore to titleImage
code = re.sub(
    r'(\s*)@Column\(name\s*=\s*"title_image"\)(\s*)private String titleImage;',
    r'\1@JsonIgnore\n\1@Column(name = "title_image")\2private String titleImage;',
    code
)

# Apply JsonIgnore to sourceImageUrl
code = re.sub(
    r'(\s*)@Column\(name\s*=\s*"source_image_url"\)(\s*)private String sourceImageUrl;',
    r'\1@JsonIgnore\n\1@Column(name = "source_image_url")\2private String sourceImageUrl;',
    code
)

# Insert the custom getters at the end of the class before the closing brace
new_getters = """
    @JsonProperty("titleImage")
    public String getTitleImageForJson() {
        if (StringUtils.isNotEmpty(this.titleContext)) {
            return null;
        }
        return this.titleImage;
    }

    @JsonProperty("sourceImageUrl")
    public String getSourceImageUrlForJson() {
        if (StringUtils.isNotEmpty(this.titleContext)) {
            return null;
        }
        return this.sourceImageUrl;
    }
}
"""

if "getTitleImageForJson" not in code:
    code = re.sub(r'}\s*$', new_getters, code)

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("Entity modified successfully.")
