package com.example.demo.service;

import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.Validation;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repository;
    private final DeviceService deviceService;

    public UserService(UserRepository repository, DeviceService deviceService) {
        this.repository = repository;
        this.deviceService = deviceService;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }

    @Transactional
    public User create(User e) {
        Validation.required(e.getName(), "name");
        Validation.required(e.getEmail(), "email");
        return repository.save(e);
    }

    @Transactional
    public User update(Long id, User e) {
        User existing = findById(id);
        if (e.getName() != null) existing.setName(e.getName());
        if (e.getEmail() != null) existing.setEmail(e.getEmail());
        return repository.save(existing);
    }

    /** Каскадное удаление: пользователь -> устройства -> наряды -> позиции. */
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("User " + id + " not found");
        }
        deviceService.deleteByOwnerId(id);
        repository.deleteById(id);
    }
}
