package br.com.brainvest.api.curriculum;

public class CurriculumNotFoundException extends RuntimeException {

    public CurriculumNotFoundException(String message) {
        super(message);
    }
}