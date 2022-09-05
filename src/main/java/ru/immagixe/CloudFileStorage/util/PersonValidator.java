package ru.immagixe.CloudFileStorage.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.immagixe.CloudFileStorage.models.Person;
import ru.immagixe.CloudFileStorage.services.PeopleService;
import ru.immagixe.CloudFileStorage.services.PersonDetailService;

@Component
public class PersonValidator implements Validator {

    private final PersonDetailService personDetailService;
    private final PeopleService peopleService;

    @Autowired
    public PersonValidator(PersonDetailService personDetailService, PeopleService peopleService) {
        this.personDetailService = personDetailService;
        this.peopleService = peopleService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Person.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Person person = (Person) target;
        if (peopleService.findByUsername(person.getUsername()).isPresent()) {
            errors.rejectValue("username", "", "Пользователь с таким именем уже существует");
        }
//        try {
//            personDetailService.loadUserByUsername(person.getUsername());
//        } catch (UsernameNotFoundException ignored) {
//            return;
//        }
    }
}
