import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class fixEntity {
    public static void main(String[] args) throws Exception {
        String path = "src/main/java/com/jlm/homework/entity/WrongTitleBook.java";
        String code = new String(Files.readAllBytes(Paths.get(path)));

        if (!code.contains("import com.fasterxml.jackson.annotation.JsonIgnore;")) {
            code = code.replace("import lombok.Data;", "import lombok.Data;\nimport com.fasterxml.jackson.annotation.JsonIgnore;\nimport com.fasterxml.jackson.annotation.JsonProperty;\nimport com.alibaba.cloud.commons.lang.StringUtils;");
        }

        code = code.replaceAll("@Column\(name = \"title_image\"\)\s*private String titleImage;", "@JsonIgnore\n    @Column(name = \"title_image\")\n    private String titleImage;");
        code = code.replaceAll("@Column\(name = \"source_image_url\"\)\s*private String sourceImageUrl;", "@JsonIgnore\n    @Column(name = \"source_image_url\")\n    private String sourceImageUrl;");

        String newGetters = "\n    @JsonProperty(\"titleImage\")\n" +
                "    public String getTitleImageForJson() {\n" +
                "        if (StringUtils.isNotEmpty(this.titleContext)) {\n" +
                "            return null;\n" +
                "        }\n" +
                "        return this.titleImage;\n" +
                "    }\n\n" +
                "    @JsonProperty(\"sourceImageUrl\")\n" +
                "    public String getSourceImageUrlForJson() {\n" +
                "        if (StringUtils.isNotEmpty(this.titleContext)) {\n" +
                "            return null;\n" +
                "        }\n" +
                "        return this.sourceImageUrl;\n" +
                "    }\n}\n";

        if (!code.contains("getTitleImageForJson")) {
            code = code.replaceAll("}\s*$", newGetters);
        }

        Files.write(Paths.get(path), code.getBytes());
        System.out.println("Entity modified successfully.");
    }
}
