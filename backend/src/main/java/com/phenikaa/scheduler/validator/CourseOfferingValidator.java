package com.phenikaa.scheduler.validator;

import com.phenikaa.scheduler.model.CourseOffering;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validator cho CourseOffering, đặc biệt là classCode
 * Format chuẩn: CSE703002-3-1-25(N01.TH1) hoặc CSE703002-3-1-25(N01)
 */
@Component
public class CourseOfferingValidator {

    // Pattern: CourseCode-x-x-x(GroupInfo) hoặc CourseCode-x-x-x(GroupInfo.ClassType)
    // Ví dụ: CSE703002-3-1-25(N01.TH1), CSE703002-3-1-25(N01)
    private static final Pattern CLASS_CODE_PATTERN = Pattern.compile(
        "^([A-Z]{3}\\d{6})(-\\d+-\\d+-\\d+)?\\(([^.\\)]+)(?:\\.([A-Z0-9]+))?\\)$"
    );

    /**
     * Validate và parse classCode
     * @param code Class code cần validate
     * @return ClassCodeInfo nếu hợp lệ, null nếu không hợp lệ
     */
    public ClassCodeInfo validateAndParse(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String trimmedCode = code.trim();
        Matcher matcher = CLASS_CODE_PATTERN.matcher(trimmedCode);

        if (!matcher.matches()) {
            return null;
        }

        ClassCodeInfo info = new ClassCodeInfo();
        info.setFullCode(trimmedCode);
        info.setCourseCode(matcher.group(1)); // CSE703002
        info.setGroupInfo(matcher.group(3));   // N01
        info.setClassTypeSuffix(matcher.group(4)); // TH1, LT, null

        // Auto-detect classType từ suffix
        if (info.getClassTypeSuffix() != null) {
            String suffix = info.getClassTypeSuffix().toUpperCase();
            if (suffix.startsWith("TH")) {
                info.setDetectedClassType("TH");
            } else if (suffix.startsWith("LT")) {
                info.setDetectedClassType("LT");
            } else if (suffix.startsWith("ELN") || suffix.contains("ONLINE")) {
                info.setDetectedClassType("ELN");
            } else {
                info.setDetectedClassType("ALL");
            }
        } else {
            info.setDetectedClassType("ALL");
        }

        return info;
    }

    /**
     * Apply auto-detection logic to CourseOffering
     */
    public void applyAutoDetection(CourseOffering offering) {
        if (offering.getCode() == null) {
            return;
        }

        ClassCodeInfo info = validateAndParse(offering.getCode());
        if (info == null) {
            return; // Invalid format, skip auto-detection
        }

        // Auto-set classType if not already set
        if (offering.getClassType() == null || offering.getClassType().isEmpty() 
            || "ALL".equals(offering.getClassType())) {
            offering.setClassType(info.getDetectedClassType());
        }
    }

    /**
     * Validate format only (không parse)
     */
    public boolean isValidFormat(String code) {
        return validateAndParse(code) != null;
    }

    /**
     * Class chứa thông tin parsed từ classCode
     */
    public static class ClassCodeInfo {
        private String fullCode;
        private String courseCode;      // CSE703002
        private String groupInfo;       // N01
        private String classTypeSuffix; // TH1, LT, null
        private String detectedClassType; // TH, LT, ELN, ALL

        public String getFullCode() { return fullCode; }
        public void setFullCode(String fullCode) { this.fullCode = fullCode; }

        public String getCourseCode() { return courseCode; }
        public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

        public String getGroupInfo() { return groupInfo; }
        public void setGroupInfo(String groupInfo) { this.groupInfo = groupInfo; }

        public String getClassTypeSuffix() { return classTypeSuffix; }
        public void setClassTypeSuffix(String classTypeSuffix) { this.classTypeSuffix = classTypeSuffix; }

        public String getDetectedClassType() { return detectedClassType; }
        public void setDetectedClassType(String detectedClassType) { this.detectedClassType = detectedClassType; }

        @Override
        public String toString() {
            return "ClassCodeInfo{" +
                    "fullCode='" + fullCode + '\'' +
                    ", courseCode='" + courseCode + '\'' +
                    ", groupInfo='" + groupInfo + '\'' +
                    ", classTypeSuffix='" + classTypeSuffix + '\'' +
                    ", detectedClassType='" + detectedClassType + '\'' +
                    '}';
        }
    }
}
