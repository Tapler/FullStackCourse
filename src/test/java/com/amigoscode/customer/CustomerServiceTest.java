package com.amigoscode.customer;

import com.amigoscode.exception.DuplicateResourceException;
import com.amigoscode.exception.RequestValidationException;
import com.amigoscode.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

//вместо autoCloseable можно использовать аннотацию @ExtendWith
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private CustomerService underTest;

    @Mock
    private CustomerDao customerDao;

    @BeforeEach
    void setUp() {
        underTest = new CustomerService(customerDao);
    }

    @Test
    void getAllCustomers() {
        // When
        underTest.getAllCustomers();
        // Then
        verify(customerDao).selectAllCustomers();
    }

    @Test
    void canGetCustomer() {
        // Given
        int id = 1;
        Customer customer = new Customer(id, "LastName FirstName SecondName", "random@email.com", 20);
        when(customerDao.selectCustomerById(id)).thenReturn(Optional.of(customer));
        // When
        Customer actual = underTest.getCustomer(id);
        // Then
        assertThat(actual).isEqualTo(customer);
    }

    @Test
    void willThrowWhenGetCustomerReturnEmptyOptional() {
        // Given
        int id = 1;
        when(customerDao.selectCustomerById(id)).thenReturn(Optional.empty());
        // When
        // Then
        assertThatThrownBy(() -> underTest.getCustomer(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("customer with id [%s] not found".formatted(id));
    }

    @Test
    void addCustomer() {
        // Given
        String email = "random@email.com";

        when(customerDao.existsPersonWithEmail(email)).thenReturn(false);

        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "LastName FirstName SecondName", email, 20
        );
        // When
        underTest.addCustomer(request);
        // Then
        ArgumentCaptor<Customer> customerArgumentCaptor = ArgumentCaptor.forClass(
                Customer.class
        );

        verify(customerDao).insertCustomer(customerArgumentCaptor.capture());

        Customer capturedCustomer = customerArgumentCaptor.getValue();

        assertThat(capturedCustomer.getId()).isNull();
        assertThat(capturedCustomer.getName()).isEqualTo(request.name());
        assertThat(capturedCustomer.getEmail()).isEqualTo(request.email());
        assertThat(capturedCustomer.getAge()).isEqualTo(request.age());
    }

    @Test
    void willThrowWhenEmailExistWhileAddingACustomer() {
        // Given
        String email = "random@email.com";

        when(customerDao.existsPersonWithEmail(email)).thenReturn(true);

        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "LastName FirstName SecondName", email, 20
        );
        // When
        assertThatThrownBy(() -> underTest.addCustomer(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("email already taken");

        // Then
        verify(customerDao, never()).insertCustomer(any());
    }

    @Test
    void deleteCustomerById() {
        // Given
        int id = 1;

        when(customerDao.existsPersonWithId(id)).thenReturn(true);
        // When
        underTest.deleteCustomerById(id);
        // Then
        verify(customerDao).deleteCustomerById(id);
    }

    @Test
    void willThrowWhenIdNotFoundWhileDeletingACustomerById() {
        // Given
        int id = 1;

        when(customerDao.existsPersonWithId(id)).thenReturn(false);
        // When
        assertThatThrownBy(() -> underTest.deleteCustomerById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("customer with id [%s] not found".formatted(id));
        // Then
        verify(customerDao, never()).deleteCustomerById(any());
    }

    @Test
    void canUpdateAllCustomerProperties() {
        // Given
        int id = 1;
        Customer customer = new Customer(id, "LastName FirstName SecondName", "random@email.com", 20);
        when(customerDao.selectCustomerById(id)).thenReturn(Optional.of(customer));

        String newEmail = "newEmail@mail.com";
        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest(
                "LastName FirstName SecondName", newEmail, 20
        );
        when(customerDao.existsPersonWithEmail(newEmail)).thenReturn(false);
        // When
        underTest.updateCustomer(id, updateRequest);
        // Then
        ArgumentCaptor<Customer> customerArgumentCaptor = ArgumentCaptor.forClass(
                Customer.class
        );
        verify(customerDao).updateCustomer(customerArgumentCaptor.capture());

        Customer capturedCustomer = customerArgumentCaptor.getValue();

        assertThat(capturedCustomer.getName()).isEqualTo(updateRequest.name());
        assertThat(capturedCustomer.getEmail()).isEqualTo(updateRequest.email());
        assertThat(capturedCustomer.getAge()).isEqualTo(updateRequest.age());
    }

    @Test
    void canUpdateOnlyCustomerName() {
        // Given
        int id = 1;
        Customer customer = new Customer(id, "LastName FirstName SecondName", "random@email.com", 20);
        when(customerDao.selectCustomerById(id)).thenReturn(Optional.of(customer));

        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest(
                "newName", null, null
        );
        // When
        underTest.updateCustomer(id, updateRequest);
        // Then
        ArgumentCaptor<Customer> customerArgumentCaptor = ArgumentCaptor.forClass(
                Customer.class
        );
        verify(customerDao).updateCustomer(customerArgumentCaptor.capture());

        Customer capturedCustomer = customerArgumentCaptor.getValue();

        assertThat(capturedCustomer.getName()).isEqualTo(updateRequest.name());
        assertThat(capturedCustomer.getEmail()).isEqualTo(customer.getEmail());
        assertThat(capturedCustomer.getAge()).isEqualTo(customer.getAge());
    }

    @Test
    void canUpdateOnlyCustomerAge() {
        // Given
        int id = 1;
        Customer customer = new Customer(id, "LastName FirstName SecondName", "random@email.com", 20);
        when(customerDao.selectCustomerById(id)).thenReturn(Optional.of(customer));

        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest(
                null, null, 23
        );
        // When
        underTest.updateCustomer(id, updateRequest);
        // Then
        ArgumentCaptor<Customer> customerArgumentCaptor = ArgumentCaptor.forClass(
                Customer.class
        );
        verify(customerDao).updateCustomer(customerArgumentCaptor.capture());

        Customer capturedCustomer = customerArgumentCaptor.getValue();

        assertThat(capturedCustomer.getName()).isEqualTo(customer.getName());
        assertThat(capturedCustomer.getEmail()).isEqualTo(customer.getEmail());
        assertThat(capturedCustomer.getAge()).isEqualTo(updateRequest.age());
    }

    @Test
    void canUpdateOnlyCustomerEmail() {
        // Given
        int id = 1;
        Customer customer = new Customer(id, "LastName FirstName SecondName", "random@email.com", 20);
        when(customerDao.selectCustomerById(id)).thenReturn(Optional.of(customer));

        String newEmail = "newEmail@mail.com";
        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest(
                null, newEmail, null
        );
        when(customerDao.existsPersonWithEmail(newEmail)).thenReturn(false);
        // When
        underTest.updateCustomer(id, updateRequest);
        // Then
        ArgumentCaptor<Customer> customerArgumentCaptor = ArgumentCaptor.forClass(
                Customer.class
        );
        verify(customerDao).updateCustomer(customerArgumentCaptor.capture());

        Customer capturedCustomer = customerArgumentCaptor.getValue();

        assertThat(capturedCustomer.getName()).isEqualTo(customer.getName());
        assertThat(capturedCustomer.getEmail()).isEqualTo(updateRequest.email());
        assertThat(capturedCustomer.getAge()).isEqualTo(customer.getAge());
    }

    @Test
    void willThrowWhenTryingToUpdateCustomerEmailWhenAlreadyTaken() {
        // Given
        int id = 1;
        Customer customer = new Customer(id, "LastName FirstName SecondName", "random@email.com", 20);
        when(customerDao.selectCustomerById(id)).thenReturn(Optional.of(customer));

        String newEmail = "newEmail@mail.com";
        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest(
                null, newEmail, null
        );
        when(customerDao.existsPersonWithEmail(newEmail)).thenReturn(true);
        // When
        assertThatThrownBy(() -> underTest.updateCustomer(id,updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("email already taken");

        // Then
        verify(customerDao, never()).updateCustomer(any());
    }

    @Test
    void willThrowWhenCustomerUpdateHasNoChanges() {
        // Given
        int id = 1;
        Customer customer = new Customer(id, "LastName FirstName SecondName", "random@email.com", 20);
        when(customerDao.selectCustomerById(id)).thenReturn(Optional.of(customer));

        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest(
                customer.getName(), customer.getEmail(), customer.getAge()
        );
        // When
        assertThatThrownBy(() -> underTest.updateCustomer(id,updateRequest))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("no data changes found");

        // Then
        verify(customerDao, never()).updateCustomer(any());

    }
}