package ru.immagixe.CloudFileStorage.security.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.immagixe.CloudFileStorage.security.models.Person;
import ru.immagixe.CloudFileStorage.security.repositories.PeopleRepository;

import java.util.Optional;

@Service
public class PeopleService {

    private final PeopleRepository peopleRepository;

    @Autowired
    public PeopleService(PeopleRepository peopleRepository) {
        this.peopleRepository = peopleRepository;
    }

    @Transactional
    public Optional<Person> findByUsername(String username) {
        Optional<Person> person = peopleRepository.findByUsername(username);
        return person;
    }
}
