---
-- Create folders for PhtoFolder test
---
 

insert into photo_collections values ( 100, 0, "subfolderTest", "", NULL, null);
insert into photo_collections values ( 101, 100, "Subfolder1", "", NULL, null);
insert into photo_collections values ( 102, 100, "Subfolder2", "", NULL, null);
insert into photo_collections values ( 103, 100, "Subfolder3", "", NULL, null);
insert into photo_collections values ( 104, 100, "Subfolder4", "", NULL, null);


insert into photo_collections values ( 105, 0, "testPhotoRetrieval", "", NULL, null);
insert into photos values( 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, "testPhotoRetrieval1" );
insert into photos values( 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, "testPhotoRetrieval2" );

insert into collection_photos values( 105, 2 );
insert into collection_photos values( 105, 3 );
