package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.LocationInfo;

import reactor.core.publisher.Mono;

public interface LocationInfoRepo extends ReactiveCrudRepository<LocationInfo, Long>{
   @Query("SELECT * FROM \"LocationInfo\" WHERE \"StateName\" = :state AND \"DistrictName\" = :district AND \"VillageName\" = :village")
    Mono<LocationInfo> findExact(
    @Param("state") String state,
    @Param("district") String district,
    @Param("village") String village);

    @Query("insert into \"LocationInfo\" (\"StateName\",\"DistrictName\",\"VillageName\")values(:StateName,:DistrictName,:VillageName) Returning *")
    Mono<LocationInfo> insert(String StateName,String DistrictName,String VillageName);

}
