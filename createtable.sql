create table xlreports(
    id int not null primary key,
    template nvarchar(250) not null,
    parameters xml,
    status int not null default 0,
    result image,
    errortext nvarchar(1024)
);