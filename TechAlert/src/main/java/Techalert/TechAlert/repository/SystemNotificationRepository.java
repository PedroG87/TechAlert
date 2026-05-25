package Techalert.TechAlert.repository;

import Techalert.TechAlert.model.SystemNotification;
import java.util.List;

import Techalert.TechAlert.model.NotificationStatus;



import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long>, JpaSpecificationExecutor<SystemNotification> {

    long countByStatus(NotificationStatus status);

    long countByStatusAndLidaFalse(NotificationStatus status);

    List<SystemNotification> findAllByOrderByDataEnvioDesc();}
