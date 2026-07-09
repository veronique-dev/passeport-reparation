import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomePageComponent } from './pages/home/home-page.component';
import { ResultPageComponent } from './pages/result/result-page.component';

const routes: Routes = [
  { path: '', component: HomePageComponent },
  { path: 'resultat', component: ResultPageComponent },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
