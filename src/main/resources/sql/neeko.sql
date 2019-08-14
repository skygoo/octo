create table "user"
(
	user_id serial not null,
	user_name varchar not null,
	password varchar not null
);

create unique index user_user_id_uindex
	on "user" (user_id);

alter table "user"
	add constraint user_pk
		primary key (user_id);

