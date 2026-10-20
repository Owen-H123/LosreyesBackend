package com.delosreyes.inventario.modules.customers.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.customers.dto.CustomerDtos;
import com.delosreyes.inventario.modules.customers.entity.Customer;
import com.delosreyes.inventario.modules.customers.mapper.CustomerMapper;
import com.delosreyes.inventario.modules.customers.repository.CustomerRepository;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.exceptions.DuplicateResourceException;
import com.delosreyes.inventario.shared.exceptions.ResourceNotFoundException;
import com.delosreyes.inventario.shared.responses.PagedResponse;

/**
 * CustomerService — Gestión de clientes.
 *
 * QUÉ HACE:
 *   CRUD de clientes con búsqueda por nombre, documento o email.
 *   Valida unicidad del número de documento.
 *
 * RELACIONES:
 *   SaleService lo usa para asociar ventas a clientes.
 */
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<CustomerDtos.Response> findAll(String search, int page, int size) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);

        Page<Customer> result = (search != null && !search.isBlank())
            ? customerRepository.search(search, pageable)
            : customerRepository.findAll(pageable);

        return PagedResponse.from(result.map(customerMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public CustomerDtos.Response findById(Long id) {
        return customerMapper.toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public CustomerDtos.Response findByDocument(String documentNumber) {
        return customerMapper.toResponse(
            customerRepository.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "documento", documentNumber))
        );
    }

    public CustomerDtos.Response create(CustomerDtos.Request request) {
        if (customerRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new DuplicateResourceException("Cliente", "documento", request.getDocumentNumber());
        }
        return customerMapper.toResponse(customerRepository.save(customerMapper.toEntity(request)));
    }

    public CustomerDtos.Response update(Long id, CustomerDtos.Request request) {
        Customer customer = findEntityById(id);

        customerRepository.findByDocumentNumber(request.getDocumentNumber()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Cliente", "documento", request.getDocumentNumber());
            }
        });

        customerMapper.updateEntity(request, customer);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    public void deactivate(Long id) {
        Customer customer = findEntityById(id);
        customer.setActive(false);
        customerRepository.save(customer);
    }

    public Customer findEntityById(Long id) {
        return customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", id));
    }
}