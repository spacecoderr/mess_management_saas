package com.thali.auth;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.thali.config.AppProperties;

/** Knows what a valid IIIT Bhubaneswar student email looks like (e.g. b423046@iiit-bh.ac.in). */
@Component
public class EmailPolicy {

    private final String domain;
    private final Pattern localPart;

    public EmailPolicy(AppProperties props) {
        this.domain = props.auth().allowedDomain().toLowerCase(Locale.ROOT);
        this.localPart = Pattern.compile(props.auth().studentLocalPartRegex());
    }

    public String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isCollegeStudentEmail(String email) {
        String e = normalize(email);
        int at = e.lastIndexOf('@');
        return at > 0 && e.substring(at + 1).equals(domain) && localPart.matcher(e.substring(0, at)).matches();
    }

    /** "b423046@iiit-bh.ac.in" -> "B423046" */
    public String rollNo(String email) {
        String e = normalize(email);
        return e.substring(0, e.indexOf('@')).toUpperCase(Locale.ROOT);
    }

    /** Roll "B423046" -> admission year 2023 (characters 3-4). Best-effort guess; students can correct it later. */
    public Short batchYear(String rollNo) {
        try {
            return (short) (2000 + Integer.parseInt(rollNo.substring(2, 4)));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
