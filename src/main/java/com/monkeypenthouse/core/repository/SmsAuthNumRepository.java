package com.monkeypenthouse.core.repository;

import com.monkeypenthouse.core.entity.SmsAuthNum;
import org.springframework.data.repository.CrudRepository;

public interface SmsAuthNumRepository extends CrudRepository<SmsAuthNum, String> {
}
