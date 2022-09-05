package ru.immagixe.CloudFileStorage.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.immagixe.CloudFileStorage.models.Person;
import ru.immagixe.CloudFileStorage.repositories.PeopleRepository;
import ru.immagixe.CloudFileStorage.security.PersonDetails;

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
