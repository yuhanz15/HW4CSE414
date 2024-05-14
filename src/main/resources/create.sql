CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (
    Username VARCHAR(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Reservations (
    ID INTEGER,
    Patient VARCHAR(255),
    Caregiver VARCHAR(255),
    Vaccine VARCHAR(255),
    Time DATE,
    FOREIGN KEY(Patient) REFERENCES Patients(Username),
    FOREIGN KEY(Caregiver) REFERENCES Caregivers(Username),
    FOREIGN KEY(Vaccine) REFERENCES Vaccines(Name)
);